package tokyo.isseikuzumaki.signalinglib.demo.server.installer.service

import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration

/**
 * Service for generating pre-signed URLs for S3 APK downloads
 */
class S3PresignedUrlService(
    private val region: String = "us-east-1"
) {
    private val logger = LoggerFactory.getLogger(S3PresignedUrlService::class.java)

    private val s3Presigner = S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build()

    /**
     * Generate a pre-signed URL for downloading an APK from S3
     * 
     * @param bucket S3 bucket name
     * @param key S3 object key (path to APK)
     * @param expirationMinutes URL expiration time in minutes (default 60)
     * @return Pre-signed URL as a string
     */
    fun generatePresignedUrl(
        bucket: String,
        key: String,
        expirationMinutes: Long = 60
    ): String {
        return try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()

            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build()

            val presignedRequest = s3Presigner.presignGetObject(presignRequest)
            val url = presignedRequest.url().toString()
            
            logger.info("Generated pre-signed URL for s3://$bucket/$key (expires in $expirationMinutes min)")
            url
        } catch (e: Exception) {
            logger.error("Failed to generate pre-signed URL for s3://$bucket/$key", e)
            throw e
        }
    }

    /**
     * Close the presigner
     */
    fun close() {
        s3Presigner.close()
    }
}
