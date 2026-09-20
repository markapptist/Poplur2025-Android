// Install-time Play Asset Delivery pack.
//
// Google Play refuses a base module whose compressed download exceeds 500 MB,
// and ours was 930 MB: the clay-map web bundle, the game and head rigs, cube
// models and banners alone are ~560 MB. An install-time pack ships with the
// app (nothing is fetched later) but is not counted against the base module,
// and its files land in the same AssetManager namespace, so every
// `file:///android_asset/...` and `context.assets.open(...)` keeps working.
//
// The files stay where every sync script writes them (app/src/main/assets);
// this module packages them through symlinks, and the app module ignores the
// same directory names so nothing ships twice.
plugins { id("com.android.asset-pack") }

assetPack {
    packName.set("claymap_assets")
    dynamicDelivery { deliveryType.set("install-time") }
}
