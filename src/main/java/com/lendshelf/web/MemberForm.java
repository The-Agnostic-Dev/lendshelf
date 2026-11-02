package com.lendshelf.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MemberForm {

    @NotBlank(message = "Please enter the member's name")
    @Size(max = 80)
    private String name;

    @NotBlank(message = "Please enter an email address")
    @Email(message = "That does not look like an email address")
    private String email;

    @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$", message = "Phone numbers can contain digits, spaces, +, ( ) and -")
    private String phone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
