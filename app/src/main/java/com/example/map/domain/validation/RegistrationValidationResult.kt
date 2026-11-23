package com.example.map.domain.validation

data class RegistrationValidationResult(
    val firstNameError: ProfileNameError? = null,
    val lastNameError: ProfileNameError? = null,
    val isEmailValid: Boolean = true,
    val passwordError: PasswordError? = null,
    val confirmPasswordError: PasswordError? = null,
    val bioError: BioError? = null
) {
    val isValid: Boolean
        get() = firstNameError == null &&
                lastNameError == null &&
                isEmailValid &&
                passwordError == null &&
                confirmPasswordError == null &&
                bioError == null
}

object RegistrationValidator {
    fun validate(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        bio: String?
    ): RegistrationValidationResult = RegistrationValidationResult(
        firstNameError = ProfileNameValidator.validate(firstName),
        lastNameError = ProfileNameValidator.validate(lastName),
        isEmailValid = EmailValidator.isValid(email),
        passwordError = PasswordValidator.validatePassword(password),
        confirmPasswordError = PasswordValidator.validateConfirmPassword(password, confirmPassword),
        bioError = BioValidator.validate(bio)
    )
}