package com.apptists.poplur2025.map

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.SystemClock
import android.util.Base64
import android.view.MotionEvent
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

/** Routes a whole touch sequence to the native map only when it began outside
 * the current real web controls. A fading control cannot steal a live drag. */
class NativeMapRoot(context: Context) : FrameLayout(context) {
    var mapTarget: View? = null
    var passthrough = false
    var regions = emptyList<RectF>()
    private var mapGesture = false
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) mapGesture = passthrough && mapTarget != null && regions.none { it.contains(event.x,event.y) }
        if (!mapGesture) return super.dispatchTouchEvent(event)
        val result = mapTarget?.dispatchTouchEvent(event) ?: false
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) mapGesture = false
        return result
    }
}

/** Native Mapbox pixels under the existing MapApp. No preview rail or labels
 * are copied here; their camera/style commands arrive from the unchanged UI. */
class NativeMapRenderer(private val context: Context, private val root: NativeMapRoot, private val web: WebView) : AutoCloseable {
    private companion object { private const val TAG = "NativeMapRenderer" }
    private var map: MapView? = null
    private var tiles: NativeMapTileServer? = null
    private var session = ""
    private var viewportWidth = 393.0
    private var viewportHeight = 852.0
    private var lastCameraSend = 0L
    private var gestureActive = false
    private val subscriptions = mutableListOf<Cancelable>()
    private val models = mutableMapOf<String,JSONObject>()
    private val pendingModels = mutableSetOf<String>()
    private var lastCameraSignature = ""
    private var presentation: JSONObject? = null

    fun receive(body: JSONObject) {
        val owner = body.optString("session")
        val commands = body.optJSONArray("commands") ?: return
        for (i in 0 until commands.length()) {
            val command = commands.optJSONObject(i) ?: continue
            try { apply(command,owner) }
            catch (error: Exception) {
                // Say it out loud. Every sibling renderer in this package logs
                // its failures; this one did not, so a map that visibly refused
                // to draw produced ZERO lines under
                // `adb logcat | grep -i nativemap`, and the only way to find out
                // why was to disassemble the Mapbox .so. One tag, one warning
                // per failed command - commands are not per-frame, so this
                // cannot become a log storm.
                Log.w(TAG, "command '${command.optString("op")}' failed: ${error.message}", error)
                emit("error",JSONObject()
                    .put("message","${command.optString("op")}: ${error.message}")
                    .put("op",command.optString("op")))
            }
        }
    }
    fun presentation(body: JSONObject) {
        presentation = body
        val active = body.optBoolean("active") && map != null
        map?.visibility = if(active) View.VISIBLE else View.GONE
        root.passthrough = active && body.optBoolean("passthrough")
        web.setBackgroundColor(if(active) android.graphics.Color.TRANSPARENT else android.graphics.Color.rgb(5,4,7))
        val width = body.optDouble("viewportWidth",393.0).coerceAtLeast(1.0); val height = body.optDouble("viewportHeight",852.0).coerceAtLeast(1.0)
        val sx = root.width/width; val sy = root.height/height
        val ox = body.optDouble("viewportOffsetX",0.0); val oy = body.optDouble("viewportOffsetY",0.0)
        val regions = body.optJSONArray("regions") ?: JSONArray()
        root.regions = (0 until regions.length()).mapNotNull { i ->
            regions.optJSONObject(i)?.let { r -> RectF(((r.optDouble("x")-ox)*sx).toFloat(),((r.optDouble("y")-oy)*sy).toFloat(),((r.optDouble("x")-ox+r.optDouble("width"))*sx).toFloat(),((r.optDouble("y")-oy+r.optDouble("height"))*sy).toFloat()) }
        }
    }
    private fun create(options: JSONObject, owner: String) {
        close(); session = owner
        options.optString("accessToken").takeIf { it.isNotEmpty() }?.let { MapboxOptions.accessToken = it }
        viewportWidth = options.optDouble("width",393.0); viewportHeight = options.optDouble("height",852.0)
        val view = MapView(context,MapInitOptions(context,cameraOptions=cameraOptions(options.optJSONObject("camera") ?: JSONObject()),styleUri=null))
        view.mapboxMap.setTileCacheBudget(TileCacheBudget(TileCacheBudgetInMegabytes(48)))
        view.setMaximumFps(30); view.compass.enabled = false; view.scalebar.enabled = false
        root.addView(view,0,FrameLayout.LayoutParams(-1,-1)); root.mapTarget = view; map = view
        tiles = NativeMapTileServer(context.cacheDir)
        view.mapboxMap.setBounds(CameraBoundsOptions.Builder().minZoom(options.optDouble("minZoom",0.0)).maxZoom(options.optDouble("maxZoom",22.0)).minPitch(0.0).maxPitch(85.0).build())
        subscriptions += view.mapboxMap.subscribeCameraChanged { publishCamera() }
        subscriptions += view.mapboxMap.subscribeMapIdle {
            publishCamera(true)
            pendingModels.toList().forEach { emit("modelReady",JSONObject().put("id",it)) }; pendingModels.clear()
            emit("render"); emit("idle")
        }
        subscriptions += view.mapboxMap.subscribeMapLoadingError {
            it.sourceId?.let { source -> pendingModels.remove(source.removePrefix("native-model-source-")) }
            emit("error",JSONObject().put("message",it.message))
        }
        subscriptions += view.mapboxMap.subscribeRenderFrameFinished { emit("render") }
        view.setOnTouchListener { _, event ->
            gestureActive = event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL
            val point=ScreenCoordinate(event.x.toDouble(),event.y.toDouble())
            val coordinate=view.mapboxMap.coordinateForPixel(point)
            val type=when(event.actionMasked) { MotionEvent.ACTION_DOWN -> "touchstart"; MotionEvent.ACTION_MOVE -> "touchmove"; else -> "touchend" }
            emit(type,JSONObject().put("gesture",true).put("point",JSONArray(listOf(point.x*viewportWidth/view.width.coerceAtLeast(1),point.y*viewportHeight/view.height.coerceAtLeast(1)))).put("coordinate",JSONArray(listOf(coordinate.longitude(),coordinate.latitude()))))
            if(event.actionMasked == MotionEvent.ACTION_DOWN) {
                web.evaluateJavascript("window.dispatchEvent(new CustomEvent('poplur-native-map-tap'));true;",null)
            }
            false
        }
        view.gestures.addOnMapClickListener { coordinate -> tap(coordinate); true }
        view.mapboxMap.loadStyle(Style.DARK) { style ->
            if (session != owner) return@loadStyle
            style.setStyleLights(value(JSONArray("""[{"id":"poplur-ambient","type":"ambient","properties":{"intensity":0.55}},{"id":"poplur-sun","type":"directional","properties":{"intensity":0.75,"direction":[210,45],"cast-shadows":false}}]""")))
            presentation?.let { presentation(it) }
            val json = JSONObject(style.styleJSON)
            emit("load",JSONObject().put("style",json)); emit("styledata")
        }
    }
    private fun publishCamera(force: Boolean = false) {
        val view = map ?: return
        val now = SystemClock.uptimeMillis()
        if (!force && now-lastCameraSend < 33) return
        lastCameraSend = now
        val camera = view.mapboxMap.cameraState
        val signature="${camera.center.longitude()},${camera.center.latitude()},${camera.zoom},${camera.pitch},${camera.bearing},${view.width},${view.height},$viewportWidth,$viewportHeight"
        if(signature==lastCameraSignature)return
        lastCameraSignature=signature
        val x = (camera.center.longitude()+180)/360; val y = (1-ln(tan(PI/4+camera.center.latitude()*PI/360))/PI)/2
        val span = 2.0.pow(-camera.zoom)*.2
        val samples = JSONArray()
        for ((dx,dy) in listOf(-1.0 to -1.0,1.0 to -1.0,1.0 to 1.0,-1.0 to 1.0)) {
            val wx=x+dx*span; val wy=y+dy*span
            val p=view.mapboxMap.pixelForCoordinate(Point.fromLngLat(wx*360-180,atan(sinh(PI*(1-2*wy)))*180/PI))
            samples.put(JSONObject().put("world",JSONArray(listOf(wx,wy))).put("screen",JSONArray(listOf(p.x*viewportWidth/view.width.coerceAtLeast(1),p.y*viewportHeight/view.height.coerceAtLeast(1)))))
        }
        emit("camera",JSONObject().put("camera",JSONObject().put("center",JSONArray(listOf(camera.center.longitude(),camera.center.latitude()))).put("zoom",camera.zoom).put("pitch",camera.pitch).put("bearing",camera.bearing)).put("projection",samples).put("gesture",gestureActive))
    }
    private fun emit(event: String, fields: JSONObject = JSONObject()) {
        if (session.isEmpty()) return
        fields.put("type","poplur-native-map-event").put("session",session).put("event",event)
        web.evaluateJavascript("(()=>{const p=$fields;function send(w){try{w.dispatchEvent(new CustomEvent('poplur-native-map-event',{detail:p}));for(const f of w.document.querySelectorAll('iframe'))send(f.contentWindow)}catch{}}send(window)})();true;",null)
    }
    /** A feature id that is genuinely present. Null (or JSON null) means the
     *  command cannot address anything, so it is dropped rather than aimed at a
     *  feature named "null". */
    private fun featureId(target: JSONObject): String? =
        target.opt("id")?.takeIf { it != JSONObject.NULL }?.toString()?.takeIf { it.isNotEmpty() }
    private fun value(raw: Any?): Value = Value.fromJson(when(raw) { is String -> JSONObject.quote(raw); null -> "null"; else -> raw.toString() }).value!!
    private fun apply(command: JSONObject, owner: String) {
        val op=command.optString("op"); val args=command.optJSONArray("args") ?: JSONArray()
        // Android's org.json is NOT JSON-java here: optString returns the
        // literal four characters "null" for a JSON null (JSONObject.NULL
        // stringifies to that and is non-null, so the fallback is unreachable).
        // The web shim sends `addLayer(layer, before ?? null)` for EVERY layer,
        // so this read turned every clay layer into "insert below the layer
        // named null" - Mapbox answered "Cannot find layer with id 'null'" and
        // refused it. The whole clay world (ground, roads, water, buildings,
        // labels, claims, discovery, pins) therefore never rendered on Android;
        // only the landmark models, which take the `model` op's own path, did.
        // The web could not tell, because the shim mirrors each layer locally
        // and getLayer() keeps answering truthy.
        fun str(i:Int)=if(args.isNull(i)) "" else args.optString(i,"")
        fun obj(i:Int)=args.optJSONObject(i) ?: JSONObject()
        if(op=="create") { create(obj(0),owner); return }
        if(owner!=session) return
        val view=map ?: return
        val style=view.mapboxMap
        fun check(result: com.mapbox.bindgen.Expected<String, com.mapbox.bindgen.None>) { result.error?.let { throw IllegalStateException(it) } }
        when(op) {
            "destroy" -> close()
            "resize" -> { viewportWidth=obj(0).optDouble("width",393.0); viewportHeight=obj(0).optDouble("height",852.0); publishCamera(true) }
            "stop" -> view.camera.cancelAllAnimators()
            "camera" -> {
                val options=obj(1); val camera=cameraOptions(options); val duration=options.optLong("duration",if(str(0)=="fly")1200 else 300)
                if(str(0)=="jump" || duration==0L) { view.camera.cancelAllAnimators(); style.setCamera(camera) }
                else if(str(0)=="fly") view.camera.flyTo(camera,MapAnimationOptions.mapAnimationOptions { duration(duration) })
                else view.camera.easeTo(camera,MapAnimationOptions.mapAnimationOptions { duration(duration) })
            }
            "fitBounds" -> {
                val points=args.optJSONArray(0) ?: return
                val coordinates=(0 until points.length()).map { val p=points.getJSONArray(it); Point.fromLngLat(p.getDouble(0),p.getDouble(1)) }
                val options=obj(1)
                val camera=style.cameraForCoordinates(coordinates,cameraOptions(options),padding(options.opt("padding")),options.optDouble("maxZoom",22.0),null)
                view.camera.easeTo(camera,MapAnimationOptions.mapAnimationOptions { duration(options.optLong("duration",600)) })
            }
            "bounds" -> {
                val options=obj(0); val b=CameraBoundsOptions.Builder()
                if(options.has("minZoom"))b.minZoom(options.getDouble("minZoom")); if(options.has("maxZoom"))b.maxZoom(options.getDouble("maxZoom"))
                if(options.has("minPitch"))b.minPitch(options.getDouble("minPitch")); if(options.has("maxPitch"))b.maxPitch(options.getDouble("maxPitch"))
                check(style.setBounds(b.build()))
            }
            "gesture" -> {
                val enabled=args.optBoolean(1)
                view.gestures.updateSettings {
                    when(str(0)) { "dragPan" -> scrollEnabled=enabled; "rotation","dragRotate" -> rotateEnabled=enabled; "touchPitch" -> pitchEnabled=enabled; "touchZoomRotate" -> pinchToZoomEnabled=enabled; "doubleClickZoom" -> doubleTapToZoomInEnabled=enabled }
                }
            }
            "addSource" -> {
                val properties=obj(1); val url=properties.optString("url")
                if(url.endsWith(".pmtiles")) { properties.remove("url"); properties.put("tiles",JSONArray(listOf(tiles!!.tiles(url)))); properties.put("maxzoom",14) }
                check(style.addStyleSource(str(0),value(properties)))
            }
            "sourceData" -> check(style.setStyleSourceProperty(str(0),"data",value(args.opt(1))))
            "removeSource" -> if(style.styleSourceExists(str(0)))check(style.removeStyleSource(str(0)))
            "addLayer" -> {
                val properties=obj(0); properties.remove("slot"); properties.optJSONObject("paint")?.remove("background-pitch-alignment")
                check(style.addStyleLayer(value(properties),str(1).takeIf { it.isNotEmpty() }?.let { LayerPosition(null,it,null) }))
            }
            "removeLayer" -> if(style.styleLayerExists(str(0)))check(style.removeStyleLayer(str(0)))
            // A null position is the sanctioned "no position" argument
            // (moveStyleLayer takes @Nullable LayerPosition); an all-null
            // LayerPosition is rejected as "Invalid LayerPosition".
            "moveLayer" -> check(style.moveStyleLayer(str(0),str(1).takeIf { it.isNotEmpty() }?.let { LayerPosition(null,it,null) }))
            "paint","layout" -> if(str(1)!="background-pitch-alignment")check(style.setStyleLayerProperty(str(0),str(1),value(args.opt(2))))
            "filter" -> check(style.setStyleLayerProperty(str(0),"filter",value(args.opt(1))))
            "image" -> {
                val data=Base64.decode(str(1).substringAfter(','),Base64.DEFAULT); val bitmap=BitmapFactory.decodeByteArray(data,0,data.size) ?: return
                view.mapboxMap.getStyle()?.addImage(str(0),bitmap,obj(2).optBoolean("sdf")); bitmap.recycle()
            }
            // `target.opt("id").toString()` is the same trap by a different
            // route: Kotlin's Any?.toString() renders a null id as "null" too,
            // so a missing id addressed a feature literally called null.
            "featureState" -> { val target=obj(0); val id=featureId(target) ?: return; style.setFeatureState(target.optString("source"),target.optString("sourceLayer").takeIf{it.isNotEmpty()},id,value(obj(1))) {} }
            "removeFeatureState" -> { val target=obj(0); val id=featureId(target) ?: return; style.removeFeatureState(target.optString("source"),target.optString("sourceLayer").takeIf{it.isNotEmpty()},id,str(1).takeIf{it.isNotEmpty()}) {} }
            // JSON null removes atmosphere; {} enables the default white fog.
            "fog" -> check(style.setStyleAtmosphere(value(if (args.isNull(0)) null else obj(0))))
            "config" -> {} // Classic styles have no Standard import configuration.
            "fov" -> {} // The native projection samples keep existing pins exact.
            "repaint" -> style.triggerRepaint()
            "query" -> query(str(0),str(1),args.opt(2),obj(3))
            "model" -> {
                val model=obj(0); val id=model.getString("id"); val asset=model.getString("asset"); require(!asset.contains(".."))
                models[id]=model
                val source=JSONObject().put("type","model").put("models",JSONObject().put(id,JSONObject().put("uri","asset://claymap/$asset").put("position",model.getJSONArray("coords"))))
                check(style.addStyleSource("native-model-source-$id",value(source)))
                val height=model.optDouble("heightM",1.0)
                val layer=JSONObject().put("id","native-model-$id").put("type","model").put("source","native-model-source-$id")
                    .put("layout",JSONObject().put("visibility",if(model.optBoolean("visible"))"visible" else "none"))
                    .put("paint",JSONObject().put("model-scale",JSONArray(listOf(height,height,height))).put("model-rotation",JSONArray(listOf(0,0,model.optDouble("yaw",0.0)))))
                check(style.addStyleLayer(value(layer),null)); pendingModels.add(id)
            }
            "modelPose" -> models[str(0)]?.let { model ->
                val source=JSONObject().put(str(0),JSONObject().put("uri","asset://claymap/${model.getString("asset")}").put("position",args.getJSONArray(1)))
                check(style.setStyleSourceProperty("native-model-source-${str(0)}","models",value(source)))
                check(style.setStyleLayerProperty("native-model-${str(0)}","model-rotation",value(JSONArray(listOf(0,0,args.getDouble(2))))))
            }
            "modelVisibility" -> check(style.setStyleLayerProperty("native-model-${str(0)}","visibility",value(if(args.optBoolean(1))"visible" else "none")))
            "removeModel" -> { models.remove(str(0)); pendingModels.remove(str(0)); if(style.styleLayerExists("native-model-${str(0)}"))check(style.removeStyleLayer("native-model-${str(0)}")); if(style.styleSourceExists("native-model-source-${str(0)}"))check(style.removeStyleSource("native-model-source-${str(0)}")) }
            else -> throw IllegalArgumentException("Unsupported map command $op")
        }
    }
    private fun query(key:String,kind:String,geometry:Any?,options:JSONObject) {
        val view=map ?: return; val owner=session
        if(kind=="source") {
            val layers=options.optString("sourceLayer").takeIf{it.isNotEmpty()}?.let { listOf(it) }
            view.mapboxMap.querySourceFeatures(geometry.toString(),SourceQueryOptions(layers,value(options.opt("filter") ?: true))) { result ->
                if(owner==session)emit("query",JSONObject().put("query",key).put("features",JSONArray(result.value?.map { feature(it.queriedFeature) } ?: emptyList<Any>())))
            }
        } else {
            val layers=options.optJSONArray("layers")?.let { a -> (0 until a.length()).map { a.getString(it) } }
            view.mapboxMap.queryRenderedFeatures(RenderedQueryGeometry(queryBox(geometry)),RenderedQueryOptions(layers,options.opt("filter")?.let(::value))) { result ->
                if(owner==session)emit("query",JSONObject().put("query",key).put("features",JSONArray(result.value?.map { feature(it.queriedFeature).put("layer",JSONObject().put("id",it.layers.firstOrNull() ?: "")) } ?: emptyList<Any>())))
            }
        }
    }
    private fun feature(item: QueriedFeature) = JSONObject(item.feature.toJson()).put("source",item.source).put("sourceLayer",item.sourceLayer)
    private fun tap(coordinate:Point) {
        val view=map ?: return; val owner=session; val point=view.mapboxMap.pixelForCoordinate(coordinate)
        view.mapboxMap.queryRenderedFeatures(RenderedQueryGeometry(point),RenderedQueryOptions(null,null)) { result ->
            if(owner!=session)return@queryRenderedFeatures
            val features=JSONArray(result.value?.map { feature(it.queriedFeature).put("layer",JSONObject().put("id",it.layers.firstOrNull() ?: "")) } ?: emptyList<Any>())
            emit("click",JSONObject().put("point",JSONArray(listOf(point.x*viewportWidth/view.width.coerceAtLeast(1),point.y*viewportHeight/view.height.coerceAtLeast(1)))).put("coordinate",JSONArray(listOf(coordinate.longitude(),coordinate.latitude()))).put("gesture",true).put("features",features))
        }
    }
    private fun queryBox(geometry:Any?):ScreenBox {
        val view=map!!; val sx=view.width/viewportWidth; val sy=view.height/viewportHeight
        if(geometry is JSONArray && geometry.optJSONArray(0)!=null) { val a=geometry.getJSONArray(0); val b=geometry.getJSONArray(1); return ScreenBox(ScreenCoordinate(a.getDouble(0)*sx,a.getDouble(1)*sy),ScreenCoordinate(b.getDouble(0)*sx,b.getDouble(1)*sy)) }
        val point=when(geometry) { is JSONArray -> geometry.optDouble(0) to geometry.optDouble(1); is JSONObject -> geometry.optDouble("x") to geometry.optDouble("y"); else -> null }
        return if(point!=null)ScreenBox(ScreenCoordinate(point.first*sx,point.second*sy),ScreenCoordinate(point.first*sx+1,point.second*sy+1)) else ScreenBox(ScreenCoordinate(0.0,0.0),ScreenCoordinate(view.width.toDouble(),view.height.toDouble()))
    }
    private fun padding(raw:Any?): EdgeInsets = when(raw) {
        is Number -> raw.toDouble().let { EdgeInsets(it,it,it,it) }
        is JSONObject -> EdgeInsets(raw.optDouble("top",0.0),raw.optDouble("left",0.0),raw.optDouble("bottom",0.0),raw.optDouble("right",0.0))
        else -> EdgeInsets(0.0,0.0,0.0,0.0)
    }
    private fun cameraOptions(options:JSONObject):CameraOptions {
        val builder=CameraOptions.Builder()
        options.optJSONArray("center")?.let { builder.center(Point.fromLngLat(it.getDouble(0),it.getDouble(1))) }
        if(options.has("zoom"))builder.zoom(options.getDouble("zoom")); if(options.has("pitch"))builder.pitch(options.getDouble("pitch")); if(options.has("bearing"))builder.bearing(options.getDouble("bearing"))
        if(options.has("padding")) {
            val edges=padding(options.opt("padding"))
            val sx=(map?.width ?: viewportWidth.toInt())/viewportWidth
            val sy=(map?.height ?: viewportHeight.toInt())/viewportHeight
            builder.padding(EdgeInsets(edges.top*sy,edges.left*sx,edges.bottom*sy,edges.right*sx))
        }
        options.optJSONArray("offset")?.let { offset -> map?.let { view -> builder.anchor(ScreenCoordinate(view.width/2.0+offset.getDouble(0)*view.width/viewportWidth,view.height/2.0+offset.getDouble(1)*view.height/viewportHeight)) } }
        return builder.build()
    }
    override fun close() {
        subscriptions.forEach { it.cancel() }; subscriptions.clear(); map?.camera?.cancelAllAnimators()
        root.mapTarget=null; root.passthrough=false; root.regions=emptyList()
        map?.let { root.removeView(it); it.onDestroy() }; map=null; tiles?.close(); tiles=null; models.clear(); pendingModels.clear(); lastCameraSignature=""; session=""
    }
}
