package com.screenmotion.app.render

import com.screenmotion.app.data.ThemeType

object SceneFactory {
    fun create(type: ThemeType): SceneRenderer = when (type) {
        ThemeType.SPACE -> SpaceSceneRenderer()
        ThemeType.AQUARIUM -> AquariumSceneRenderer()
        ThemeType.VEHICLE -> VehicleSceneRenderer()
    }
}
