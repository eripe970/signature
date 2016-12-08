package com.signicat.signature;

import com.signicat.document.v3.*;
import org.apache.http.client.fluent.Executor;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.ws.Service;
import java.io.File;
import java.io.IOException;

public class RequireAuthenticationBeforeSigning {
    @Test
    public void you_may_choose_to_require_that_the_user_authenticates_before_signing() throws Exception {
        SdsDocument uploadedDocument = uploadDocument();
        CreateRequestRequest request = getCreateRequest(uploadedDocument);

        request.getRequest().get(0).getTask().get(0).setSubject(getSubject());
        Authentication authentication = new Authentication();
        authentication.getMethod().add("nemid");
        request.getRequest().get(0).getTask().get(0).setAuthentication(authentication);

        Service documentService = new DocumentService();
        DocumentEndPoint client = documentService.getPort(DocumentEndPoint.class);
        CreateRequestResponse response = client.createRequest(request);

        String signHereUrl =
                String.format("https://preprod.signicat.com/std/docaction/demo?request_id=%s&task_id=%s",
                        response.getRequestId().get(0), request.getRequest().get(0).getTask().get(0).getId());

        System.out.println(signHereUrl);
        Assert.assertNotNull(response);
        Assert.assertNull(response.getArtifact());
        Assert.assertNotNull(response.getRequestId());
    }

    private SdsDocument uploadDocument() throws IOException {
        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request
                .Post("https://preprod.signicat.com/doc/demo/sds")
                .bodyFile(new File("src/test/resources/demo_avtale.pdf"), ContentType.create("application/pdf"));
        String documentId = Executor.newInstance()
                .auth("demo", "Bond007")
                .execute(request)
                .returnContent().asString();

        SdsDocument sdsDocument = new SdsDocument();
        sdsDocument.setId("doc_1");
        sdsDocument.setRefSdsId(documentId);
        sdsDocument.setDescription("Terms and conditions");
        return sdsDocument;
    }


    public Subject getSubject() {
        Subject subject = new Subject();
        subject.setId("subj_1"); // Any identifier you'd like
        subject.setNationalId("1909740939"); // CPR, personnummer, f?dselsnummer etc.
        return subject;
    }

    private CreateRequestRequest getCreateRequest(SdsDocument documentInSds) {
        CreateRequestRequest createRequestRequest = new CreateRequestRequest();
        createRequestRequest.setService("demo");
        createRequestRequest.setPassword("Bond007");

        Request request = new Request();
        request.setClientReference("cliref1");
        request.setLanguage("da");
        request.setProfile("demo");

        request.getDocument().add(documentInSds);

        Task task = new Task();
        task.setId("task_1"); // Any identifier you'd like
        task.setSubjectRef("subj_1"); // Any identifier you'd like
        task.setBundle(false);

        DocumentAction documentAction = new DocumentAction();
        documentAction.setType(DocumentActionType.SIGN); // Sign or View
        documentAction.setDocumentRef("doc_1"); // Any identifier you'd like
        task.getDocumentAction().add(documentAction);

        Signature signature = new Signature();
        signature.getMethod().add("nemid-sign"); // The name of the signature method(s)
        task.getSignature().add(signature);

        request.getTask().add(task);

        createRequestRequest.getRequest().add(request);
        return createRequestRequest;
    }
}
