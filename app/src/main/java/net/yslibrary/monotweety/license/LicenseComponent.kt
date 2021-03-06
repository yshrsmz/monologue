package net.yslibrary.monotweety.license

import dagger.Component
import net.yslibrary.monotweety.UserComponent
import net.yslibrary.monotweety.base.di.ControllerScope

@ControllerScope
@Component(
    dependencies = [UserComponent::class],
    modules = [LicenseViewModule::class]
)
interface LicenseComponent {
    fun inject(controller: LicenseController)
}
