package net.yslibrary.monotweety.ui.splash

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import net.yslibrary.monotweety.R
import net.yslibrary.monotweety.analytics.Analytics
import net.yslibrary.monotweety.databinding.FragmentSplashBinding
import net.yslibrary.monotweety.ui.base.ViewBindingFragment
import net.yslibrary.monotweety.ui.base.consumeEffects
import net.yslibrary.monotweety.ui.base.consumeStates
import net.yslibrary.monotweety.ui.base.navigateSafe
import net.yslibrary.monotweety.ui.di.HasComponent
import net.yslibrary.monotweety.ui.di.ViewModelFactory
import net.yslibrary.monotweety.ui.di.getComponentProvider
import timber.log.Timber
import javax.inject.Inject

class SplashFragment : ViewBindingFragment<FragmentSplashBinding>(
    R.layout.fragment_splash,
    FragmentSplashBinding::bind
), HasComponent<SplashFragmentComponent> {

    override val component: SplashFragmentComponent by lazy {
        requireActivity().getComponentProvider<SplashFragmentComponent.ComponentProvider>()
            .splashFragmentComponent()
            .build()
    }

    @Inject
    lateinit var factory: ViewModelFactory<SplashViewModel>

    @Inject
    lateinit var analytics: Analytics

    private val viewModel: SplashViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        component.inject(this)

        viewLifecycleOwnerLiveData.observe(this) { owner ->
            viewModel.consumeEffects(owner.lifecycleScope, this::handleEffect)
            viewModel.consumeStates(owner.lifecycleScope, this::render)
        }
        viewModel.dispatch(SplashIntent.Initialize)

        analytics.screenView(Analytics.Screen.Splash, this::class)
    }

    private fun handleEffect(effect: SplashEffect) {
        Timber.d("newEffect: $effect")
        when (effect) {
            SplashEffect.ToLogin -> {
                findNavController().navigateSafe(SplashFragmentDirections.toLogin())
            }
            SplashEffect.ToMain -> {
                findNavController().navigateSafe(SplashFragmentDirections.toMain())
                activity?.finish()
            }
        }
    }

    private fun render(state: SplashState) {
        Timber.d("newState: $state")
    }
}
