package com.example.contactapi.service;

import com.example.contactapi.domain.Contact;
import com.example.contactapi.repo.ContactRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.example.contactapi.constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ContactService {
    private  final ContactRepo contactRepo;

    public Page<Contact> getAllContacts(int page, int size){
        return contactRepo.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    public  Contact getContact(String id){
        return contactRepo.findById(id).orElseThrow(() ->new RuntimeException("Contact not Found"));
    }

    public  Contact createContact(Contact contact){
        return contactRepo.save(contact);
    }
    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Saving picture for user ID: {}",id);
        Contact contact = getContact(id);
        String photoUrl = photoFunction.apply(id, file);
        contact.setPhotoUrl(photoUrl);
        contactRepo.save(contact);
        return photoUrl;
    }

    private final Function<String, String> fileExtension = filename -> Optional.of(filename)
            .filter(name -> name.contains("."))
            .map(name -> "." + name.substring(name.lastIndexOf(".") + 1))
            .orElse(".png");

    private final BiFunction<String,MultipartFile,String> photoFunction=(id,image) ->{
        try{
            Path fileStorageLocation= Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if (!Files.exists(fileStorageLocation)){Files.createDirectories(fileStorageLocation);}
            Files.copy(image.getInputStream(),fileStorageLocation.resolve(id+fileExtension.apply(image.getOriginalFilename())),REPLACE_EXISTING);
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/contacts/image/"+id+fileExtension.apply(image.getOriginalFilename())).toUriString();
        }catch(Exception exception){
            throw new RuntimeException("Unable to save Image");
        }
    };

}
