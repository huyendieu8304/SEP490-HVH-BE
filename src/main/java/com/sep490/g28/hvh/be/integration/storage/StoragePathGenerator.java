package com.sep490.g28.hvh.be.integration.storage;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility component for generating standardized storage paths.
 * <p>
 * Centralizes path conventions to avoid hard-coded strings.
 * </p>
 */
@Component
public class StoragePathGenerator {

    private static final String IDENTITY_VERIFICATION_FOLDER = "/identity-verification";
    private static final String ORG_REGISTRATION_FOLDER = "/org-registration";
    private static final String EVENT_FOLDER = "/event";

    private static final String EVENT_IMAGE_DIR = "/image";
    private static final String EVENT_CLAIMS_FILE_NAME = "/claims_";
    private static final String EVENT_MOMENTS_FILE_NAME = "/moments_";

    private static final String CID_FRONT_FILE_NAME = "/cid-front";
    private static final String CID_BACK_FILE_NAME = "/cid-back";
    private static final String CID_HOLDING_FILE_NAME = "/cid-holding";
    private final static String LEGAL_DOCUMENTS_FILE_NAME = "/legal-docs_";
    private static final String OTHER_EVIDENCES_FILE_NAME = "/others_";



    public String identityVerificationCidFront(UUID verificationId, String fileExtension){
        return  IDENTITY_VERIFICATION_FOLDER + "/" + verificationId + CID_FRONT_FILE_NAME + fileExtension;
    }

    public String identityVerificationCidBack(UUID verificationId, String fileExtension){
        return  IDENTITY_VERIFICATION_FOLDER + "/" + verificationId + CID_BACK_FILE_NAME + fileExtension;
    }

    public String identityVerificationCidHolding(UUID verificationId, String fileExtension){
        return  IDENTITY_VERIFICATION_FOLDER + "/" + verificationId + CID_HOLDING_FILE_NAME + fileExtension;
    }

    //================================================================================================

    public String orgRegistrationCidFront(UUID registrationId, String fileExtension){
        return  ORG_REGISTRATION_FOLDER+ "/" + registrationId + CID_FRONT_FILE_NAME + fileExtension;
    }

    public String orgRegistrationCidBack(UUID registrationId, String fileExtension) {
        return  ORG_REGISTRATION_FOLDER+ "/" + registrationId + CID_BACK_FILE_NAME + fileExtension;
    }

    public String orgRegistrationCidHolding(UUID registrationId, String fileExtension) {
        return  ORG_REGISTRATION_FOLDER+ "/" + registrationId + CID_HOLDING_FILE_NAME + fileExtension;
    }

    public String orgRegistrationLegalDocuments(UUID registrationId, int order, String fileExtension) {
        return  ORG_REGISTRATION_FOLDER+ "/" + registrationId + LEGAL_DOCUMENTS_FILE_NAME + order + fileExtension;
    }

    public String orgRegistrationOtherEvidences(UUID registrationId, int order, String fileExtension) {
        return  ORG_REGISTRATION_FOLDER+ "/" + registrationId + OTHER_EVIDENCES_FILE_NAME + order + fileExtension;
    }

    //================================================================================================
    public String eventImage (UUID eventId, UUID imageId,  String fileExtension) {
        return EVENT_FOLDER + "/" + eventId + EVENT_IMAGE_DIR + "/" + imageId + fileExtension;
    }

    public String eventClaimImages(UUID applicationId, int order, String fileExtension) {
        return  EVENT_FOLDER + "/" + applicationId + EVENT_CLAIMS_FILE_NAME + order + fileExtension;
    }

    public String eventMomentImages(UUID applicationId, int order, String fileExtension) {
        return  EVENT_FOLDER + "/" + applicationId + EVENT_MOMENTS_FILE_NAME + order + fileExtension;
    }

}
