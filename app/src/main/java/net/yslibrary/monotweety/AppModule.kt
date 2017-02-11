package net.yslibrary.monotweety

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.twitter.sdk.android.core.TwitterSession
import dagger.Module
import dagger.Provides
import net.yslibrary.monotweety.base.Clock
import net.yslibrary.monotweety.base.ClockImpl
import net.yslibrary.monotweety.base.RefWatcherDelegate
import net.yslibrary.monotweety.base.RefWatcherDelegateImpl
import net.yslibrary.monotweety.base.di.AppScope
import net.yslibrary.monotweety.base.di.Names
import rx.lang.kotlin.PublishSubject
import rx.subjects.PublishSubject
import javax.inject.Named

/**
 * Created by yshrsmz on 2016/09/24.
 */
@Module
open class AppModule(private val context: Context) {

  @Named(Names.FOR_APP)
  @AppScope
  @Provides
  fun provideAppContext(): Context = context

  @AppScope
  @Provides
  open fun provideAppLifecycleCallbacks(@Named(Names.FOR_APP) context: Context): App.LifecycleCallbacks {
    return AppLifecycleCallbacks(context)
  }

  @AppScope
  @Provides
  fun provideNotificationManager(@Named(Names.FOR_APP) context: Context): NotificationManagerCompat {
    return NotificationManagerCompat.from(context)
  }

  @AppScope
  @Provides
  fun providePackageManager(@Named(Names.FOR_APP) context: Context): PackageManager {
    return context.packageManager
  }

  @AppScope
  @Provides
  fun provideClock(): Clock {
    return ClockImpl()
  }

  @AppScope
  @Provides
  fun provideConfig(): Config {
    return Config.init()
  }

  @AppScope
  @Provides
  open fun provideRefWatcher(): RefWatcher {
    return LeakCanary.install(context.applicationContext as Application)
  }

  @Provides
  fun provideAnalytics(@Named(Names.FOR_APP) context: Context): FirebaseAnalytics {
    return FirebaseAnalytics.getInstance(context)
  }

  @Named(Names.FOR_LOGIN)
  @AppScope
  @Provides
  fun provideLoginCompletedSubject(): PublishSubject<TwitterSession> {
    return PublishSubject()
  }

  @Provides
  fun provideRefWatcherDelegate(refWatcherDelegateImpl: RefWatcherDelegateImpl): RefWatcherDelegate {
    return refWatcherDelegateImpl
  }

  interface Provider {
    fun notificationManager(): NotificationManagerCompat
    fun clock(): Clock
    fun config(): Config
    fun refWatcherDelegate(): RefWatcherDelegate
    @Named(Names.FOR_LOGIN)
    fun loginCompletedSubject(): PublishSubject<TwitterSession>
  }
}