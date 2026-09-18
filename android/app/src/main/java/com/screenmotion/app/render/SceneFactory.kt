package com.screenmotion.app.render

import com.screenmotion.app.data.ThemeType
import com.screenmotion.app.data.VehicleType

object SceneFactory {
    fun create(type: ThemeType, vehicle: VehicleType = VehicleType.SPORTS_CAR): SceneRenderer =
        when (type) {
            ThemeType.SPACE -> SpaceSceneRenderer()
            ThemeType.AQUARIUM -> AquariumSceneRenderer()
            ThemeType.VEHICLE -> VehicleSceneRenderer(vehicle)
            ThemeType.NATURE -> NatureSceneRenderer()
        }
}
