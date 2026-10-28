package cl.duoc.tienda.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class S3StorageService {
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.of(
        MediaType.IMAGE_JPEG_VALUE, "jpg",
        MediaType.IMAGE_PNG_VALUE, "png",
        "image/webp", "webp"
    );
    private final S3Client s3;
    private final String bucket;

    public S3StorageService(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public String uploadProductImage(Long productId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen debe tener entre 1 byte y 5 MB");
        }
        String mediaType = file.getContentType();
        String extension = TYPES.get(mediaType);
        if (extension == null) throw new IllegalArgumentException("Formato permitido: JPEG, PNG o WebP");
        byte[] bytes = file.getBytes();
        if (!matchesSignature(mediaType, bytes)) throw new IllegalArgumentException("El archivo no corresponde al formato declarado");
        String key = "productos/" + productId + "/" + UUID.randomUUID() + "." + extension;
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(mediaType).build(),
                RequestBody.fromBytes(bytes));
        return key;
    }

    private boolean matchesSignature(String type, byte[] b) {
        if (type.equals(MediaType.IMAGE_JPEG_VALUE)) return b.length > 2 && (b[0]&255)==0xff && (b[1]&255)==0xd8 && (b[2]&255)==0xff;
        if (type.equals(MediaType.IMAGE_PNG_VALUE)) return b.length > 7 && (b[0]&255)==137 && b[1]==80 && b[2]==78 && b[3]==71 && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10;
        return b.length > 11 && b[0]=='R' && b[1]=='I' && b[2]=='F' && b[3]=='F' && b[8]=='W' && b[9]=='E' && b[10]=='B' && b[11]=='P';
    }

    public byte[] download(String key) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    }

    public String contentType(String key) {
        return s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).contentType();
    }

    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
