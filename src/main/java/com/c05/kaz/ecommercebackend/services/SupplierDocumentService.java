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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplierDocumentService {

    private final Cloudinary cloudinary;
    private final SupplierRepository supplierShopRepository;
    private final SupplierDocumentRepository supplierDocumentRepository;

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
                                "folder", "supplier-docs/" + supplierId
                        )
                );

                String url = (String) uploadResult.get("secure_url");

                SupplierDocument doc = SupplierDocument.builder()
                        .supplier(shop)
                        .fileUrl(url)
                        .type(DocumentType.OTHER)   // hoặc detect type nếu muốn
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
}
