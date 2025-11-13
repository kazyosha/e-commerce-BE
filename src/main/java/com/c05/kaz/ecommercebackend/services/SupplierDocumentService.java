package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.SupplierDocument;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.enums.DocumentType;
import com.c05.kaz.ecommercebackend.repository.SupplierDocumentRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SupplierDocumentService {

    private final Cloudinary cloudinary;
    private final SupplierRepository supplierShopRepository;
    private final SupplierDocumentRepository supplierDocumentRepository;

    /** Upload document lên Cloudinary */
    public List<SupplierDocument> uploadDocuments(Long supplierId, List<MultipartFile> files) {
        SupplierShop shop = supplierShopRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        List<SupplierDocument> result = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                Map uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "supplier-docs/" + supplierId,
                                "resource_type", "auto"
                        )
                );

                String url = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");

                SupplierDocument doc = SupplierDocument.builder()
                        .supplier(shop)
                        .fileUrl(url)
                        .publicId(publicId)
                        .type(DocumentType.OTHER)
                        .uploadedAt(LocalDateTime.now())
                        .build();

                supplierDocumentRepository.save(doc);
                result.add(doc);

            } catch (IOException e) {
                throw new RuntimeException("Upload to Cloudinary failed", e);
            }
        }

        if (result.isEmpty()) {
            throw new RuntimeException("No valid files uploaded");
        }

        return result;
    }

    /** Xoá toàn bộ file Cloudinary + record trong DB */
    public void deleteDocumentsBySupplier(Long supplierId) {
        List<SupplierDocument> docs = supplierDocumentRepository.findBySupplier_Id(supplierId);

        for (SupplierDocument doc : docs) {

            try {
                cloudinary.uploader().destroy(doc.getPublicId(), ObjectUtils.emptyMap());
            } catch (Exception ignored) {}

        }

        supplierDocumentRepository.deleteAll(docs);
    }
}
