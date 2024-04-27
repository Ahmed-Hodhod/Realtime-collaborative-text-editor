package com.alibou.security.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "http://localhost:8082")

@RestController
@RequestMapping("/api")
public class DocumentController {

    @Autowired
    DocumentRepository documentRepository;

    @PostMapping("/documents")
    public ResponseEntity<Document> createDocument(HttpServletRequest request) {
        try {

            // Log request headers

            System.out.println(request);

            /////////////

            // show the request body in the console

            return new ResponseEntity<>(null, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/documents/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable("id") long id) {
        System.out.println("Before.");
        Optional<Document> documentData = documentRepository.findById(id);
        System.out.println("After.");

        if (documentData.isPresent()) {
            return new ResponseEntity<>(documentData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
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

}