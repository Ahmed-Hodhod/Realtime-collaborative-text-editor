package com.alibou.security.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import com.alibou.security.document.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;

@CrossOrigin(origins = "http://localhost:8082")

@RestController
@RequestMapping("/api")
public class DocumentController {

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DocumentPermissionRepository dpr;

    @Value("${application.security.jwt.secret-key}")
    private String secret;

    @PostMapping("/documents")
    public ResponseEntity<Document> createDocument(HttpServletRequest request,
            @RequestBody NewDocRequest newDocRequest) {
        try {

            // get the currently logged in user
            String authorizationHeader = request.getHeader("Authorization");

            String token = authorizationHeader.substring(7); // Assuming the token starts with "Bearer "
            System.out.println(authorizationHeader);

            Claims claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
            String email = (String) claims.get("sub");
            Optional<User> userOptional = userRepository.findByEmail(email);

            User user = userOptional.get();

            Document newDocument = new Document(user, newDocRequest.getTitle());
            Document doc = documentRepository.save(newDocument);

            user.addDocument(doc);
            user = userRepository.save(user);

            System.out.println(userOptional.get().getDocuments());
            return new ResponseEntity<>(HttpStatus.CREATED);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            System.out.println("Finally Block.");
        }
    }

    @PostMapping("/document/{documentId}/share")
    public ResponseEntity<String> ShareDocument(HttpServletRequest request, @RequestBody ShareRequest shareRequest,
            @PathVariable Long documentId) {
        try {

            Long userId = shareRequest.getUserId();
            PermissionType permissionType = PermissionType.valueOf(shareRequest.getPermissionType());

            // get the currently logged in user
            String authorizationHeader = request.getHeader("Authorization");

            String token = authorizationHeader.substring(7); // Assuming the token starts with "Bearer "
            System.out.println(authorizationHeader);

            Claims claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
            String email = (String) claims.get("sub");
            Optional<User> userOptional = userRepository.findByEmail(email);

            User user = userOptional.get();

            // check the validity of both the document and the user you are sharing with
            Optional<User> sharingWithUserOptional = userRepository.findById(userId);
            Optional<Document> documentOptional = documentRepository.findById(documentId);

            if (!sharingWithUserOptional.isPresent() || !documentOptional.isPresent()) {
                return new ResponseEntity<>(
                        "Invalid user or document. Make sure to share a valid document with an existing user. ",
                        HttpStatus.BAD_REQUEST);
            }

            User sharingWithUser = sharingWithUserOptional.get();
            Document document = documentOptional.get();

            // check if the user is the owner of the document
            if (document.getOwner().getId() != user.getId()) {
                return new ResponseEntity<>("You are not the owner of this document.", HttpStatus.FORBIDDEN);
            }

            // check if the user is sharing with theirself
            if (user.getId() == sharingWithUser.getId()) {
                return new ResponseEntity<>("You can't share this document with yourself.", HttpStatus.FORBIDDEN);
            }

            // check if the permission entry already exists and in this case update it only
            Optional<DocumentPermission> permissionOptional = dpr.findById(new CompositeKey(documentId, userId));

            if (permissionOptional.isPresent()) {
                DocumentPermission permission = permissionOptional.get();
                if (permission.getPermissionType() == permissionType) {
                    return ResponseEntity.ok("This user already has this permission on this document.");
                }

                permission.setPermissionType(permissionType);
                dpr.save(permission);
                return ResponseEntity.ok("Permission updated successfully.");
            }

            // create a new permission entry and save it
            DocumentPermission newPermission = new DocumentPermission();
            newPermission.setDocument(documentId);
            newPermission.setUser(userId);

            CompositeKey id = new CompositeKey(documentId, userId);
            newPermission.setId(id);
            newPermission.setPermissionType(permissionType);

            dpr.save(newPermission);

            return ResponseEntity.ok("Document shared successfully with the user");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {

            System.out.println("Finally Block.");
        }

    }
}

// @GetMapping("/documents")
// public ResponseEntity<List<Document>> getAllDocuments(HttpServletRequest
// request) {
// try {
// // Log request headers
// String authorizationHeader = request.getHeader("Authorization");
// String token = authorizationHeader.substring(7); // Assuming the token starts
// with "Bearer "

// Claims claims =
// Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
// String email = (String) claims.get("sub");
// Optional<User> userOptional = userRepository.findByEmail(email);

// User user = userOptional.get();
// List<Document> documents = user.getDocuments();

// return new ResponseEntity<>(documents, HttpStatus.OK);
// } catch (Exception e) {
// return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
// }
// }

// @GetMapping("/documents/{id}")
// public ResponseEntity<Document> getDocumentById(@PathVariable("id") long id,
// HttpServletRequest request) {
// try {
// // Log request headers
// String authorizationHeader = request.getHeader("Authorization");
// String token = authorizationHeader.substring(7); // Assuming the token starts
// with "Bearer "

// Claims claims =
// Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
// String email = (String) claims.get("sub");
// Optional<User> userOptional = userRepository.findByEmail(email);

// User user = userOptional.get();
// Optional<Document> documentData = documentRepository.findById(id);

// if (documentData.isPresent()) {
// Document doc = documentData.get();
// if (doc.getUser().getId() == user.getId()) {
// return new ResponseEntity<>(doc, HttpStatus.OK);
// } else {
// return new ResponseEntity<>(HttpStatus.FORBIDDEN);
// }
// } else {
// return new ResponseEntity<>(HttpStatus.NOT_FOUND);
// }
// } catch (Exception e) {
// return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
// }
// }

// @PutMapping("/documents/{id}")
// public ResponseEntity<Document> updateDocument(@PathVariable("id") long id,
// @RequestBody Document document,
// HttpServletRequest request) {
// try {
// // Log request headers
// String authorizationHeader = request.getHeader("Authorization");
// String token = authorizationHeader.substring(7); // Assuming the token starts
// with "Bearer "

// Claims claims =
// Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
// String email = (String)
// }
// }

// @GetMapping("/documents/{id}")
// public ResponseEntity<Document> getDocumentById(@PathVariable("id") long id)
// {
// System.out.println("Before.");
// Optional<Document> documentData = documentRepository.findById(id);
// System.out.println("After.");

// if (documentData.isPresent()) {
// return new ResponseEntity<>(documentData.get(), HttpStatus.OK);
// } else {
// return new ResponseEntity<>(HttpStatus.NOT_FOUND);
// }
// }
//
// @putmapping("/tutorials/{id}")
// public responseentity<tutorial> updatetutorial(@pathvariable("id") long id,
// @requestbody tutorial tutorial) {
// optional<tutorial> tutorialdata = tutorialrepository.findbyid(id);
//
// if (tutorialdata.ispresent()) {
// tutorial _tutorial = tutorialdata.get();
// _tutorial.settitle(tutorial.gettitle());
// _tutorial.setdescription(tutorial.getdescription());
// _tutorial.setpublished(tutorial.ispublished());
// return new responseentity<>(tutorialrepository.save(_tutorial),
// httpstatus.ok);
// } else {
// return new responseentity<>(httpstatus.not_found);
// }
// }
//
// @deletemapping("/tutorials/{id}")
// public responseentity<httpstatus> deletetutorial(@pathvariable("id") long id)
// {
// try {
// tutorialrepository.deletebyid(id);
// return new responseentity<>(httpstatus.no_content);
// } catch (exception e) {
// return new responseentity<>(httpstatus.internal_server_error);
// }
// }

// @deletemapping("/tutorials")
// public responseentity<httpstatus> deletealltutorials() {
// try {
// tutorialrepository.deleteall();
// return new responseentity<>(httpstatus.no_content);
// } catch (exception e) {
// return new responseentity<>(httpstatus.internal_server_error);
// }
//
// }