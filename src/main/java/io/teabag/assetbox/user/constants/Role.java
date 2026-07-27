package io.teabag.assetbox.user.constants;

public enum Role {
    USER("ROLE"), ADMIN("admin"), SUPER_ADMIN("superadmin");
    private String value;
    Role(String value){
        this.value = value;
    }
}