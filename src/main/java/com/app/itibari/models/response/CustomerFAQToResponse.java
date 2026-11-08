package com.app.itibari.models.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CustomerFAQToResponse(UUID publicId,
                                    String creationTime,
                                    String createdBy,
                                    Department department,
                                    List<CustomerFAQNote> noteList) {
    @Builder
    public record Department(UUID publicId, String name) {
    }

    @Builder
    public record CustomerFAQNote(UUID publicId,
                                  String creationTime,
                                  String question,
                                  String answer,
                                  IssueType issueType) {

        @Builder
        public record IssueType(UUID publicId, String creationTime, String name) {
        }
    }


}


// {
//            "publicId": "256811d4-657c-4fe6-8e23-6ccc63939bca",
//            "creationTime": "2026-04-08 12:53:51",
//            "department": {
//                "publicId": "4a40b827-4a82-467d-9e00-dce4006896e0",
//                "creationTime": "2026-04-01 11:48:07",
//                "name": "Engineering"
//            },
//            "noteList": [
//                {
//                    "publicId": "64a30ca3-76bf-4a18-8ea6-a76e969cfd1e",
//                    "creationTime": "2026-04-09 14:23:55",
//                    "question": null,
//                    "answer": "yes, testing.",
//                    "issueType": {
//                        "publicId": "eea39e59-194c-4ca7-b600-0f7bed66dec9",
//                        "creationTime": "2026-03-25 04:08:35",
//                        "name": "Mismatch Amounts"
//                    }
//                },
//                {
//                    "publicId": "204b0601-0dcc-428a-bf6b-fb0ec14a3256",
//                    "creationTime": "2026-04-08 12:53:52",
//                    "question": "Ask Tech",
//                    "answer": "We have notes",
//                    "issueType": {
//                        "publicId": "d8fa89ed-e1a5-4ff4-822b-a141edef50b6",
//                        "creationTime": "2026-04-08 12:42:40",
//                        "name": "System lag"
//                    }
//                },
//                {
//                    "publicId": "fdfdd0f2-cee6-4c12-a584-69ed57dbb32c",
//                    "creationTime": "2026-04-10 10:01:30",
//                    "question": "testing?",
//                    "answer": "yes, testing",
//                    "issueType": {
//                        "publicId": "9e0a9e94-8bdf-4271-9ad4-08814cfcd1e1",
//                        "creationTime": "2026-04-09 13:47:15",
//                        "name": "System lag"
//                    }
//                }
//            ]
//        }
