[![REUSE status](https://api.reuse.software/badge/github.com/SAP/sap-customer-data-cloud-sdk-for-android)](https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android)

# Description

SAP Customer Data Cloud SDK for Android. The SDK provides an interface to integrate SAP Customer Data Cloud services in your Android application.


# Requirements

Android 24+


# Implementation

The library is available on [MavenCentral](http://sap.com)


    implementation("")

# SDK Setup


## SiteConfig

The `SiteConfig` class is designed to encapsulate and manage relevant site-specific data, such as the API key, domain, and other configuration parameters. This class plays a pivotal role in the initialization of the authentication service. By having a `SiteConfig` instance, the authentication service can access necessary site-specific details that are essential for its functionality, ensuring proper configuration and secure operations. Without this class, the authentication service would lack the required settings to authenticate users and interact with the API effectively.


    val siteConfig = SiteConfig(context)

The `SiteConfig` class is designed to automatically fetch necessary resources from the `strings.xml` file when provided with the appropriate context. This enables the class to retrieve essential configuration data, such as API keys and domain names, seamlessly.

These are the available configuration parameters required for the `SiteConfig` class:


    <!-- mandatory -->
    <string name="com.sap.cxcdc.apikey">API_KEY_HERE</string>
    <!-- optional - if not specified will default to "us1.gigya.com" -->
    <string name="com.sap.cxcdc.domain">API_DOMAIN_HERE</string> 
    <!-- optional -->
    <string name="com.sap.cxcdc.cname">CNAME_HERE</string>

Additionally, the `SiteConfig` class supports multiple build flavors. By placing flavor-specific `strings.xml` files in the corresponding suffix paths defined in the `build.gradle` application configuration, developers can manage different configurations for various flavors efficiently. This approach enhances flexibility and organization, allowing for easy maintenance and configuration management across different environments or flavors of the application.

## AuthenticationService

The `AuthenticationService` class serves as the primary interface for managing and executing all authentication-related operations within the SDK. This class is the central entry point for clients to interact with various authentication flows, data retrieval, data updates, session management, and the resolution of interrupted authentication processes.


### Key Methods:


*   **authenticate**() :

    **Purpose**: Handle all types of authentication API flows.

    **Usage**: Clients can use this method to initiate authentication processes, whether it involves username/password authentication, social sign-in, single sign-on, or any other supported mechanism.

*   **get():**

    **Purpose**: Retrieve specific authentication-related data.

    **Usage**: Clients can utilize this method to fetch specific details about the current authentication state, user profiles, or any other relevant data

*   **set():**

    **Purpose**: Update specific authentication-related data.

    **Usage**: This method is designed for modifying various authentication parameters, such as updating account/profile information.

*   **resolve():**

    **Purpose**: Resolve interrupted authentication flows.

    **Usage**: Clients can employ this method to handle cases where an authentication flow was interrupted, possibly due to missing requirements, account linking, or other anomalies.

*   **session():**

    **Purpose**: Manage session-specific interactions

    **Usage**: This method enables clients to interact with user sessions, including session creation, validation, termination, and retrieval of session-specific data.


By leveraging the `AuthenticationService` class, clients can efficiently manage and streamline their authentication needs, ensuring a robust and secure authentication process within the SDK.


    /**
    * Initialize authentication service.
    */
    private var authenticationService =AuthenticationService(siteConfig)



# Authentication Flows

The SDK is designed with a focus on authentication flows, ensuring a seamless user experience. Whether the user initiates a simple or complex authentication process, our SDK streamlines the entire experience by providing clear pathways for resolution in case of interruptions.

Minimizing frustration, developers can implement flexible authentication solutions that adapt to different user needs while maintaining a smooth and efficient workflow.


**Below is a code example demonstrating a simple credentials authentication flow using our SDK, which allows users to easily log in and handle interruptions during the process:**


    val params = mutableMapOf("email" to email, "password" to password)
    val authResponse: IAuthResponse = authenticationService.authenticate().register(params)

# Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports, etc. via \[GitHub issues\]([https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/issues](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/issues) ). Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project structure, and additional contribution information, see our \[Contribution Guidelines\]([CONTRIBUTING.md](http://CONTRIBUTING.md) ).


# Security / Disclosure

If you find any bug that may be a security problem, please follow our instructions at \[in our security policy\]([https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/security/policy](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/security/policy) ) on how to report it. Please do not create GitHub issues for security-related doubts or problems.


# Code of Conduct

As members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to always abide by its \[Code of Conduct\]([https://github.com/SAP/.github/blob/main/CODE\_OF\_CONDUCT.md](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md) ).


# Licensing

Copyright 2024 SAP SE or an SAP affiliate company and sap-customer-data-cloud-sdk-for-android contributors. Please see our \[LICENSE\](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available \[via the REUSE tool\]([https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android](https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android) ).