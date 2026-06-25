package com.apptists.poplur2025.wear.gdx

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector3

// ── Vertex shader ─────────────────────────────────────────────────────────────

private const val VERT = """
attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;

varying vec2  v_uv;
varying vec3  v_normal;
varying vec3  v_worldPos;

void main() {
    vec4 worldPos  = u_worldTrans * a_position;
    v_worldPos     = worldPos.xyz;
    v_normal       = normalize(u_normalMatrix * a_normal);
    v_uv           = a_texCoord0;
    gl_Position    = u_projViewTrans * worldPos;
}
"""

// ── Fragment shader ────────────────────────────────────────────────────────────
//
// Blinn-Phong reflectance with an ocean specular mask, Fresnel atmospheric rim,
// and a subtle night-side city-glow warmth — all in one pass.

private const val FRAG = """
#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_diffuse;
uniform vec3 u_lightDir;      // world-space direction TO the sun (normalised)
uniform vec3 u_lightColor;
uniform vec3 u_ambientColor;
uniform vec3 u_cameraPos;     // world-space camera position

varying vec2  v_uv;
varying vec3  v_normal;
varying vec3  v_worldPos;

void main() {
    vec4 tex  = texture2D(u_diffuse, v_uv);

    vec3 N    = normalize(v_normal);
    vec3 L    = normalize(u_lightDir);
    vec3 V    = normalize(u_cameraPos - v_worldPos);
    vec3 H    = normalize(L + V);           // Blinn-Phong half-vector

    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.0);

    // ---------- specular mask ----------
    // Ocean pixels are blue-dominant; land pixels are green-dominant.
    // Clamp to [0,1] so the mask is 0 on land, 1 deep in the ocean.
    float oceanMask = clamp((tex.b - tex.g * 0.85) * 3.0, 0.0, 1.0);

    // Ocean: glossy mirror-like sheen (shininess 96)
    // Land:  very dull matte (shininess 6)
    float shininess = mix(6.0, 96.0, oceanMask);
    float specAmt   = pow(max(dot(N, H), 0.0), shininess) * oceanMask * 1.6;

    // ---------- Fresnel / atmosphere ----------
    // Edge glow — blue-white atmospheric scattering at the limb.
    float fresnel    = pow(1.0 - NdotV, 4.5);
    vec3  atmo       = vec3(0.22, 0.52, 1.0) * fresnel * (0.45 + NdotL * 0.55);

    // ---------- night-side city warmth ----------
    // A faint warm tint bleeds through on the dark side.
    float nightFrac  = 1.0 - smoothstep(-0.08, 0.25, NdotL);
    vec3  nightGlow  = tex.rgb * vec3(1.0, 0.55, 0.20) * nightFrac * 0.18;

    // ---------- assemble ----------
    vec3 ambient  = u_ambientColor * tex.rgb;
    vec3 diffuse  = u_lightColor   * tex.rgb * NdotL;
    vec3 specular = u_lightColor   * vec3(1.0, 0.97, 0.88) * specAmt;

    gl_FragColor  = vec4(ambient + diffuse + specular + atmo + nightGlow, 1.0);
}
"""

// ── Custom shader ──────────────────────────────────────────────────────────────

private class GlobeShader : Shader {

    private var program: ShaderProgram? = null
    private var uProjViewTrans  = -1
    private var uWorldTrans     = -1
    private var uNormalMatrix   = -1
    private var uDiffuse        = -1
    private var uLightDir       = -1
    private var uLightColor     = -1
    private var uAmbientColor   = -1
    private var uCameraPos      = -1

    private val normalMat = Matrix3()
    // Direction FROM the sun (matches iOS: euler -0.4, -0.6 → approx this vector)
    private val lightDir  = Vector3(-0.39f, -0.59f, -0.71f).nor()

    override fun init() {
        ShaderProgram.pedantic = false
        val prog = ShaderProgram(VERT, FRAG)
        if (!prog.isCompiled) {
            Gdx.app.error("GlobeShader", "Compile error:\n${prog.log}")
            return
        }
        program         = prog
        uProjViewTrans  = prog.getUniformLocation("u_projViewTrans")
        uWorldTrans     = prog.getUniformLocation("u_worldTrans")
        uNormalMatrix   = prog.getUniformLocation("u_normalMatrix")
        uDiffuse        = prog.getUniformLocation("u_diffuse")
        uLightDir       = prog.getUniformLocation("u_lightDir")
        uLightColor     = prog.getUniformLocation("u_lightColor")
        uAmbientColor   = prog.getUniformLocation("u_ambientColor")
        uCameraPos      = prog.getUniformLocation("u_cameraPos")
    }

    override fun compareTo(other: Shader): Int = 0
    override fun canRender(instance: Renderable): Boolean = true

    override fun begin(camera: Camera, context: RenderContext) {
        val prog = program ?: return
        prog.bind()
        context.setDepthTest(GL20.GL_LEQUAL)
        context.setDepthMask(true)
        prog.setUniformMatrix(uProjViewTrans, camera.combined)
        prog.setUniformf(uCameraPos,    camera.position.x, camera.position.y, camera.position.z)
        prog.setUniformf(uLightDir,     lightDir.x, lightDir.y, lightDir.z)
        prog.setUniformf(uLightColor,   1.00f, 0.97f, 0.90f)   // warm sun
        prog.setUniformf(uAmbientColor, 0.22f, 0.22f, 0.22f)
    }

    override fun render(renderable: Renderable) {
        val prog = program ?: return
        prog.setUniformMatrix(uWorldTrans, renderable.worldTransform)

        // Normal matrix = (worldTrans⁻¹)ᵀ  (upper-left 3×3)
        normalMat.set(renderable.worldTransform).inv().transpose()
        prog.setUniformMatrix(uNormalMatrix, normalMat)

        val texAttr = renderable.material.get(TextureAttribute.Diffuse) as? TextureAttribute
        texAttr?.textureDescription?.texture?.let { tex ->
            tex.bind(0)
            prog.setUniformi(uDiffuse, 0)
        }

        renderable.meshPart.render(prog, true)
    }

    override fun end() {}

    override fun dispose() {
        program?.dispose()
        program = null
    }
}

// ── Shader provider ────────────────────────────────────────────────────────────

private class GlobeShaderProvider : ShaderProvider {
    private var shader: GlobeShader? = null

    override fun getShader(renderable: Renderable): Shader {
        return shader ?: GlobeShader().also { s ->
            s.init()
            shader = s
        }
    }

    override fun dispose() {
        shader?.dispose()
        shader = null
    }
}

// ── ApplicationAdapter ────────────────────────────────────────────────────────

class GlobeGdxAdapter : ApplicationAdapter() {
    private var camera: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var globeModel: Model? = null
    private var globeInstance: ModelInstance? = null
    private var earthTexture: Texture? = null
    private var yRotation = 0f

    override fun create() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(0f, 0.2f, 3.0f)
            lookAt(0f, 0f, 0f)
            near = 0.5f
            far  = 300f
            update()
        }

        modelBatch = ModelBatch(GlobeShaderProvider())

        earthTexture = try {
            Texture(Gdx.files.internal("earth_texture.png")).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        } catch (e: Exception) {
            Gdx.app.error("GlobeGdxAdapter", "Failed to load earth_texture.png", e)
            null
        }

        val material = Material().apply {
            if (earthTexture != null) set(TextureAttribute.createDiffuse(earthTexture))
            else                      set(ColorAttribute.createDiffuse(Color.BLUE))
        }

        globeModel = ModelBuilder().createSphere(
            2f, 2f, 2f, 48, 48,
            material,
            (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()
        )
        globeInstance = ModelInstance(globeModel)
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        yRotation += 30f * Gdx.graphics.deltaTime   // 12-second full rotation

        globeInstance?.transform?.idt()
            ?.rotate(Vector3.Z, -23.5f)              // Earth's axial tilt
            ?.rotate(Vector3.Y, yRotation)

        modelBatch?.begin(camera)
        modelBatch?.render(globeInstance)            // no Environment — shader owns lighting
        modelBatch?.end()
    }

    override fun dispose() {
        modelBatch?.dispose()
        globeModel?.dispose()
        earthTexture?.dispose()
    }

    override fun resize(width: Int, height: Int) {
        camera?.viewportWidth  = width.toFloat()
        camera?.viewportHeight = height.toFloat()
        camera?.update()
    }
}
