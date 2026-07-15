package com.skripsi.core.di

import android.content.Context
import com.skripsi.core.data.benchmark.CameraRoiBenchmarkReportExporter
import com.skripsi.core.data.benchmark.CameraRoiBenchmarkRunner
import com.skripsi.core.data.image.ImagePreprocessor
import com.skripsi.core.data.image.RoiCropper
import com.skripsi.core.data.repository.ProduceRepositoryImpl
import com.skripsi.core.domain.repository.ProduceRepository
import com.skripsi.core.domain.usecase.AnalyzeProduceUseCase
import com.skripsi.core.domain.usecase.ExportCameraBenchmarkReportUseCase
import com.skripsi.core.domain.usecase.GetRoiConfigurationUseCase
import com.skripsi.core.domain.usecase.RunCameraRoiBenchmarkUseCase
import com.skripsi.tflite_engine.TFLiteClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideTFLiteClassifier(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context
    ): TFLiteClassifier {
        return TFLiteClassifier(context)
    }

    @Provides
    @Singleton
    fun provideProduceRepository(
        implementation: ProduceRepositoryImpl
    ): ProduceRepository {
        return implementation
    }

    @Provides
    @Singleton
    fun provideCameraRoiBenchmarkRunner(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
        tfliteClassifier: TFLiteClassifier,
        roiCropper: RoiCropper,
        imagePreprocessor: ImagePreprocessor
    ): CameraRoiBenchmarkRunner {
        return CameraRoiBenchmarkRunner(context, tfliteClassifier, roiCropper, imagePreprocessor)
    }

    @Provides
    @Singleton
    fun provideCameraRoiBenchmarkReportExporter(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context
    ): CameraRoiBenchmarkReportExporter {
        return CameraRoiBenchmarkReportExporter(context)
    }

    @Provides
    @Singleton
    fun provideAnalyzeProduceUseCase(
        produceRepository: ProduceRepository
    ): AnalyzeProduceUseCase {
        return AnalyzeProduceUseCase(produceRepository)
    }

    @Provides
    @Singleton
    fun provideGetRoiConfigurationUseCase(): GetRoiConfigurationUseCase {
        return GetRoiConfigurationUseCase()
    }

    @Provides
    @Singleton
    fun provideRunCameraRoiBenchmarkUseCase(
        runner: CameraRoiBenchmarkRunner
    ): RunCameraRoiBenchmarkUseCase {
        return RunCameraRoiBenchmarkUseCase(runner)
    }

    @Provides
    @Singleton
    fun provideExportCameraBenchmarkReportUseCase(
        exporter: CameraRoiBenchmarkReportExporter
    ): ExportCameraBenchmarkReportUseCase {
        return ExportCameraBenchmarkReportUseCase(exporter)
    }

}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PerformanceTestEntryPoint {
    fun produceRepository(): com.skripsi.core.domain.repository.ProduceRepository
}
