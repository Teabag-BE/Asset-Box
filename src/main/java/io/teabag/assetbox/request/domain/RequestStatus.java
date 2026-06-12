package io.teabag.assetbox.request.domain;

public enum RequestStatus {
    /*
    * 현재 파일에서는 5가지로 분류하지만, 실제로는 3가지로 진행중 :
    *   REQUESTED(요청됨), IN_PROGRESS(진행중), COMPLETED(완료됨)
    * */
    REQUESTED, IN_REVIEW, IN_PROGRESS, COMPLETED, REJECTED
}
