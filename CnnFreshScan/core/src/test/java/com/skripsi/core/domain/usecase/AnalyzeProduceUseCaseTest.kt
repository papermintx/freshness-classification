package com.skripsi.core.domain.usecase

import android.graphics.Bitmap
import com.skripsi.core.domain.model.ProduceClassificationResult
import com.skripsi.core.domain.repository.ProduceRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Unit Tests for AnalyzeProduceUseCase
 * 
 * Tests the produce analysis use case logic
 * REQ-02: Verify that inference returns correct classifications
 * REQ-03: Verify gallery image analysis works correctly
 */
class AnalyzeProduceUseCaseTest {

    private lateinit var useCase: AnalyzeProduceUseCase
    private lateinit var produceRepository: ProduceRepository

    @Before
    fun setUp() {
        produceRepository = mockk()
        useCase = AnalyzeProduceUseCase(produceRepository)
    }

    /**
     * TEST: Verify use case invokes repository with correct bitmap
     * Expected: Use case should call repository's analyzeImage method
     */
    @Test
    fun testUseCaseInvokesRepository() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val expectedResults = listOf(
            ProduceClassificationResult(name = "banana", accuracyScore = 0.95f)
        )
        
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns expectedResults

        // Act
        val result = useCase.invoke(mockBitmap)

        // Assert
        assertEquals("Use case should return repository results", expectedResults, result)
    }

    /**
     * TEST: Verify multiple classification results are returned
     * Expected: Use case should return all classification results from repository
     * Requirement: REQ-02 - Must handle multiple classifications correctly
     */
    @Test

        fun testMultipleClassificationResults() = runTest {
            // Arrange
            val mockBitmap = mockk<Bitmap>(relaxed = true)
            val multipleResults = listOf(
                ProduceClassificationResult(name = "apple", accuracyScore = 0.85f),
                ProduceClassificationResult(name = "banana", accuracyScore = 0.10f),
                ProduceClassificationResult(name = "strawberry", accuracyScore = 0.05f)
            )
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns multipleResults

        // Act
        val results = useCase.invoke(mockBitmap)

        // Assert
        assertEquals("Should return all three classification results", 3, results.size)
        assertTrue("Should contain apple classification", results.any { it.name == "apple" })
        assertTrue("Should contain banana classification", results.any { it.name == "banana" })
    }

    /**
     * TEST: Verify high confidence scores are properly handled
     * Expected: High confidence scores should be returned as-is
     */
    @Test
    fun testHighConfidenceScores() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val highConfidenceResults = listOf(
            ProduceClassificationResult(name = "apple", accuracyScore = 0.99f)
        )
        
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns highConfidenceResults

        // Act
        val results = useCase.invoke(mockBitmap)

        // Assert
        assertTrue("High confidence should be preserved", results[0].accuracyScore > 0.98f)
    }

    /**
     * TEST: Verify low confidence scores are properly handled
     * Expected: Low confidence scores should still be returned
     */
    @Test
    fun testLowConfidenceScores() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val lowConfidenceResults = listOf(
            ProduceClassificationResult(name = "unknown_produce", accuracyScore = 0.15f)
        )
        
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns lowConfidenceResults

        // Act
        val results = useCase.invoke(mockBitmap)

        // Assert
        assertEquals("Low confidence should be preserved", 0.15f, results[0].accuracyScore)
    }

    /**
     * TEST: Verify use case works with gallery images
     * Expected: Use case should handle bitmap from gallery selection
     * Requirement: REQ-03 - Gallery image selection must trigger inference pipeline
     */
    @Test
    fun testGalleryImageAnalysis() = runTest {
        // Arrange: Simulate gallery image bitmap
        val galleryBitmap = mockk<Bitmap>(relaxed = true)
        val galleryResults = listOf(
            ProduceClassificationResult(name = "strawberry", accuracyScore = 0.88f)
        )
        
        coEvery { produceRepository.analyzeImage(galleryBitmap) } returns galleryResults

        // Act
        val results = useCase.invoke(galleryBitmap)

        // Assert
        assertEquals("Should return classification for gallery image", 1, results.size)
        assertEquals("Should correctly classify gallery image", "strawberry", results[0].name)
    }

    /**
     * TEST: Verify empty results handling
     * Expected: Use case should handle empty classification results
     */
    @Test
    fun testEmptyResults() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val emptyResults = emptyList<ProduceClassificationResult>()
        
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns emptyResults

        // Act
        val results = useCase.invoke(mockBitmap)

        // Assert
        assertTrue("Empty results should be handled", results.isEmpty())
    }

    /**
     * TEST: Verify different bitmap sizes are accepted
     * Expected: Use case should work with various bitmap dimensions
     */
    @Test
    fun testVariousBitmapSizes() = runTest {
        // Test different bitmap configurations
        val bitmapConfigs = listOf(224, 512, 640) // Different sizes
        
        for (size in bitmapConfigs) {
            // Arrange
            val bitmap = mockk<Bitmap>(relaxed = true) {
                every { width } returns size
                every { height } returns size
            }
            
            val results = listOf(ProduceClassificationResult(name = "apple", accuracyScore = 0.90f))
            coEvery { produceRepository.analyzeImage(bitmap) } returns results

            // Act
            val result = useCase.invoke(bitmap)

            // Assert
            assertNotNull("Should handle bitmap of size $size", result)
        }
    }

    /**
     * TEST: Verify confidence scores are between 0 and 1
     * Expected: All confidence scores should be valid percentages
     */
    @Test
    fun testConfidenceScoreValidity() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val results = listOf(
            ProduceClassificationResult(name = "apple", accuracyScore = 0.95f),
            ProduceClassificationResult(name = "banana", accuracyScore = 0.05f),
            ProduceClassificationResult(name = "orange", accuracyScore = 0.00f)
        )
        
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns results

        // Act
        val classificationResults = useCase.invoke(mockBitmap)

        // Assert
        for (result in classificationResults) {
            assertTrue(
                "Confidence score must be between 0 and 1, got ${result.accuracyScore}",
                result.accuracyScore in 0f..1f
            )
        }
    }

    /**
     * TEST: Verify produce labels are not empty
     * Expected: All returned produce labels should be valid strings
     */
    @Test
    fun testProduceLabelsValidity() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val results = listOf(
            ProduceClassificationResult(name = "apple", accuracyScore = 0.95f),
            ProduceClassificationResult(name = "banana", accuracyScore = 0.85f)
        )
        
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns results

        // Act
        val classificationResults = useCase.invoke(mockBitmap)

        // Assert
        for (result in classificationResults) {
            assertTrue("Produce label should not be empty", result.name.isNotEmpty())
        }
    }

    /**
     * TEST: Verify use case operator function works
     * Expected: Using invoke operator should call the use case properly
     */
    @Test
    fun testOperatorInvoke() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val results = listOf(ProduceClassificationResult(name = "apple", accuracyScore = 0.92f))
        
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns results

        // Act - Using operator syntax
        val result = useCase(mockBitmap)

        // Assert
        assertEquals("Operator invoke should work correctly", results, result)
    }

    /**
     * TEST: Verify sorted results by confidence
     * Expected: Results should reflect repository's ordering
     */
    @Test
    fun testConfidenceSorting() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val unsortedResults = listOf(
            ProduceClassificationResult(name = "banana", accuracyScore = 0.50f),
            ProduceClassificationResult(name = "apple", accuracyScore = 0.95f),
            ProduceClassificationResult(name = "orange", accuracyScore = 0.20f)
        )
        
        coEvery { produceRepository.analyzeImage(mockBitmap) } returns unsortedResults

        // Act
        val results = useCase.invoke(mockBitmap)

        // Assert
        assertEquals("Results order should match repository response", unsortedResults, results)
    }
}

