package com.alibou.security.document;

import java.util.ArrayList;
import java.util.Collections;
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
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;

@CrossOrigin(origins = "http://localhost:3000")
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

    @CrossOrigin(origins = "http://localhost:3000")

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

            String userEmail = shareRequest.getUserEmail();
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
            Optional<User> sharingWithUserOptional = userRepository.findByEmail(userEmail); // findById(userId);
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
            Optional<DocumentPermission> permissionOptional = dpr
                    .findById(new CompositeKey(documentId, sharingWithUser.getId()));

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
            newPermission.setUser(sharingWithUser.getId());

            CompositeKey id = new CompositeKey(documentId, sharingWithUser.getId());
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

    // get all documents of the currently logged in user whether they are the owner
    // or they have been shared with

    // This was the error: jakarta.servlet.ServletException: Unable to handle the
    // Spring Security Exception because the response is already committed.
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(HttpServletRequest request) {
        try {

            // get the currently logged in user
            String authorizationHeader = request.getHeader("Authorization");

            String token = authorizationHeader.substring(7); // Assuming the token starts with "Bearer "
            System.out.println(authorizationHeader);

            Claims claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
            String email = (String) claims.get("sub");
            Optional<User> userOptional = userRepository.findByEmail(email);

            User user = userOptional.get();

            List<DocumentResponse> objects = new ArrayList<>();
            List<DocumentPermission> shared_docs = dpr.findByUser(user.getId());
            // get the shared list of documents

            for (DocumentPermission document : shared_docs) {
                Document doc = documentRepository.findById(document.getDocument()).get();
                System.out.println("Document: " + doc.getTitle() + " Permission: " + document.getPermissionType() +
                        " User: " + document.getUser());

                objects.add(new DocumentResponse(document.getDocument(), document.getUser(), doc.getTitle(),
                        document.getPermissionType()));
            }

            for (Document document : user.getDocuments()) {

                PermissionType permission = null;
                if (document.getOwner().getId() == user.getId()) {
                    permission = PermissionType.OWNER;
                } else {
                    Optional<DocumentPermission> permissionOptional = dpr
                            .findById(new CompositeKey(document.getId(), user.getId()));
                    permission = permissionOptional.get().getPermissionType();
                }

                System.out.println("Permission: " + permission);

                objects.add(new DocumentResponse(document.getId(), user.getId(), document.getTitle(), permission));
            }

            return new ResponseEntity<>(objects, HttpStatus.OK);

        } catch (JwtException e) {
            System.out.println("JWT Exception: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            System.out.println("Internal Server Error: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            System.out.println("Finally Block.");
        }
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponse> getDocument(HttpServletRequest request, @PathVariable Long documentId) {
        try {

            // get the currently logged in user
            String authorizationHeader = request.getHeader("Authorization");

            String token = authorizationHeader.substring(7); // Assuming the token starts with "Bearer "
            System.out.println(authorizationHeader);

            Claims claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
            String email = (String) claims.get("sub");
            Optional<User> userOptional = userRepository.findByEmail(email);

            User user = userOptional.get();

            DocumentResponse resultDoc = null;

            // find the document in the list of shared documents
            List<DocumentPermission> shared_docs = dpr.findByUser(user.getId());
            for (DocumentPermission document : shared_docs) {
                if (document.getDocument() == documentId) {
                    Document doc = documentRepository.findById(document.getDocument()).get();
                    resultDoc = new DocumentResponse(document.getDocument(), document.getUser(), doc.getTitle(),
                            document.getPermissionType());
                }
            }

            // find the document in the owned documents
            for (Document document : user.getDocuments()) {

                if (documentId == document.getId()) {
                    resultDoc = new DocumentResponse(document.getId(), user.getId(), document.getTitle(),
                            PermissionType.OWNER);
                }

            }
            if (resultDoc != null) {
                return new ResponseEntity<>(resultDoc, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

        } catch (JwtException e) {
            System.out.println("JWT Exception: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            System.out.println("Internal Server Error: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            System.out.println("Finally Block.");
        }
    }

    @PutMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponse> RenameDocument(HttpServletRequest request, @PathVariable Long documentId,
            @RequestBody RenameRequest renamRequest) {
        try {

            // get the currently logged in user
            String authorizationHeader = request.getHeader("Authorization");

            String token = authorizationHeader.substring(7); // Assuming the token starts with "Bearer "
            System.out.println(authorizationHeader);

            Claims claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
            String email = (String) claims.get("sub");
            Optional<User> userOptional = userRepository.findByEmail(email);

            User user = userOptional.get();

            DocumentResponse resultDoc = null;

            // find the document in the list of shared documents
            List<DocumentPermission> shared_docs = dpr.findByUser(user.getId());
            for (DocumentPermission document : shared_docs) {
                if (document.getDocument() == documentId) {
                    if (document.getPermissionType() == PermissionType.EDIT) {
                        Document doc = documentRepository.findById(document.getDocument()).get();

                        doc.setTitle(renamRequest.getTitle());
                        documentRepository.save(doc);

                        System.out.println(doc.getTitle());

                        resultDoc = new DocumentResponse(document.getDocument(), document.getUser(),
                                renamRequest.getTitle(),
                                document.getPermissionType());
                    } else {
                        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
                    }
                }
            }

            // find the document in the owned documents
            for (Document document : user.getDocuments()) {

                if (documentId == document.getId()) {
                    document.setTitle(renamRequest.getTitle());
                    documentRepository.save(document);
                    System.out.println(renamRequest.getTitle());
                    resultDoc = new DocumentResponse(document.getId(), user.getId(), renamRequest.getTitle(),
                            PermissionType.OWNER);
                }

            }
            if (resultDoc != null) {
                return new ResponseEntity<>(resultDoc, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

        } catch (JwtException e) {
            System.out.println("JWT Exception: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            System.out.println("Internal Server Error: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            System.out.println("Finally Block.");
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<String> RenameDocument(HttpServletRequest request, @PathVariable Long documentId) {
        try {

            // get the currently logged in user
            String authorizationHeader = request.getHeader("Authorization");

            String token = authorizationHeader.substring(7); // Assuming the token starts with "Bearer "
            System.out.println(authorizationHeader);

            Claims claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
            String email = (String) claims.get("sub");
            Optional<User> userOptional = userRepository.findByEmail(email);

            User user = userOptional.get();

            // delete the document only if you are the owner
            for (Document document : user.getDocuments()) {

                if (documentId == document.getId()) {
                    documentRepository.delete(document);
                    return new ResponseEntity<>("Deleted Succesfully!", HttpStatus.OK);
                }

            }
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);

        } catch (JwtException e) {
            System.out.println("JWT Exception: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            System.out.println("Internal Server Error: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            System.out.println("Finally Block.");
        }
    }

    @PostMapping("/user")
    public ResponseEntity<UserData> GetUserInformation(HttpServletRequest request,
            @RequestBody GetUserRequest getUserRequest) {
        try {

            // get the currently logged in user
            String authorizationHeader = request.getHeader("Authorization");

            String token = authorizationHeader.substring(7); // Assuming the token starts with "Bearer "
            System.out.println(authorizationHeader);

            Claims claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();

            // find by email
            if (getUserRequest.getUserEmail() != null) {
                User user = userRepository.findByEmail(getUserRequest.getUserEmail()).get();

                return new ResponseEntity<>(
                        new UserData(user.getFirstname(), user.getLastname(), user.getEmail(), user.getId()),
                        HttpStatus.OK);
            }

            // find by ID
            if (getUserRequest.getUserId() != null) {
                User user = userRepository.findById(getUserRequest.getUserId()).get();

                return new ResponseEntity<>(
                        new UserData(user.getFirstname(), user.getLastname(), user.getEmail(), user.getId()),
                        HttpStatus.OK);
            }

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        } catch (JwtException e) {
            System.out.println("JWT Exception: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            System.out.println("Internal Server Error: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            System.out.println("Finally Block.");
        }
    }

}

// Define a simple class representing your object
class DocumentResponse {
    private String title;
    private PermissionType permissionType;
    private Long ownerId;
    private long docId;

    public DocumentResponse(long docId, Long ownerId, String title, PermissionType permissionType) {
        this.title = title;
        this.permissionType = permissionType;
        this.ownerId = ownerId;
        this.docId = docId;
    }

    public PermissionType getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(PermissionType permissionType) {
        this.permissionType = permissionType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDocId() {
        return docId;
    }

    public void setDocId(long docId) {
        this.docId = docId;
    }

}

class UserData {
    private String firstname;
    private String lastname;
    private String email;
    private Long id;

    public UserData(String firstname, String lastname, String email, Long id) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.id = id;
    }

    public String getFirstName() {
        return firstname;
    }

    public String getLastName() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstname) {
        this.firstname = firstname;
    }

    public void setLastName(String lastname) {
        this.lastname = lastname;
    }
}