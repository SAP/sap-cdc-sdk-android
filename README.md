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


    // ... Initialize the configuration object.
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


    // ... Initialize authentication service.
    private var authenticationService = AuthenticationService(siteConfig)



# Authentication Flows

The SDK is designed with a focus on authentication flows, ensuring a seamless user experience. Whether the user initiates a simple or complex authentication process, our SDK streamlines the entire experience by providing clear pathways for resolution in case of interruptions.

Minimizing frustration, developers can implement flexible authentication solutions that adapt to different user needs while maintaining a smooth and efficient workflow.


**Example:**
    
    // ... Create your credential information map
    val params = mutableMapOf("email" to email, "password" to password)

    // ... Use the "register" authentication inteface to register a new user
    val authResponse: IAuthResponse = authenticationService.authenticate().register(params)



## IAuthResponse

The IAuthResponse interface provides the relevant authentication state through its properties and methods, allowing you to determine the outcome of an authentication operation.


**Key elements for determining authentication state:**

*  **state():**

Returns an AuthState enum value (ERROR, SUCCESS, INTERRUPTED) indicating the overall authentication state.

*  **cdcResponse():**

Provides access to the underlying CDCResponse object, which contains detailed information about the API response, including error codes and messages.

*  **resolvable():**

If the state() is INTERRUPTED, this method returns a ResolvableContext object containing data needed to resolve the interruption and continue the authentication flow.

By examining these elements, you can determine if the authentication was successful, encountered an error, or requires further action.


**Example:**


    val authResponse: IAuthResponse = // ... perform authentication operation
    
    when (authResponse.state()) {
        AuthState.SUCCESS -> {
            // Authentication successful, proceed with the application flow
        }
        AuthState.ERROR -> {
            // Authentication failed, handle the error
            val error = authResponse.toDisplayError()
            // ... display error message or take corrective action
        }
        AuthState.INTERRUPTED -> {
            // Authentication interrupted, resolve the issue
            val resolvableContext = authResponse.resolvable()
            // ... use resolvableContext to gather additional information or perform necessary steps
        }
    }

## Resolving Interruptions

During an authentication flow, certain errors may require additional steps to be taken by the user before the flow can be completed. These errors are considered "resolvable" and are indicated by an AuthState.INTERRUPTED state in the IAuthResponse object.
To resolve an interrupted authentication flow, you need to utilize the AuthenticationResolve interface. This interface provides methods for gathering the necessary information from the user and resuming the authentication process.

**Steps to Resolve an Interrupted Flow**

1. **Identify the interruption:** When an authentication operation returns an IAuthResponse with AuthState.INTERRUPTED, it means the flow has been interrupted. Determine the error using the "authResponse.cdcResponse().errorCode()" method. 

2. **Gather required information:** The ResolvableContext object contains details about the interruption, including the missing required fields or any other information needed to resolve the issue. Use this information to prompt the user for the necessary input. You can access the ResolvableContext object using the resolvable() method of the IAuthResponse.

3. **Call to resolve**: Use the resolve() method of the AuthenticationResolve interface, using the correct resolve method for the specific interruption error.

**Example:**


    val authResponse: IAuthResponse = // ... perform authentication operation
    
    if (authResponse.state() == AuthState.INTERRUPTED) {
        // ... Determine the error
        when (authResponse.cdcResponse().errorCode()) {
               ResolvableContext.ERR_ACCOUNT_PENDING_REGISTRATION -> {
                  val resolvableContext = authResponse.resolvable()
                  // ... prompt the user for missing required fields based on resolvableContext
                  navigateToSpecificViewToResolveInterruption(resolvableContext)
               }
        }
    }

When the information is available, call the relevant resolve interface.

    // ... Create a map of serialized parameters (missing profile fields for example).
    val params = mutableMapOf(key to serializedJsonValue)

    // ... Use the provided resolve interface to resolve the interruption.
    val resolveResponse =  authenticationService.resolve().pendingRegistrationWith(regToken, params)

# Web Screen-Sets

Web Screen-Sets and Integration with Android

Web Screen Sets are dynamic web user interfaces that allow for customized authentication flows. They provide a flexible and customizable way to design and implement user login and registration experiences.

The SDK utilizes the WebBridgeJS object to connect the Android application to Web ScreenSets. This object creates a JavaScript bridge between the native application and a WebView element running the CDC JavaScript SDK.

**How it Works:**


1.  WebView: The Android application uses a WebView element to display the Web Screen-Sets. The WebView acts as a container for the web-based UI.

2.  CDC JavaScript SDK.: The Web Screen-Sets are built using the JS CDC SDK, which provides the necessary JavaScript functions and components for authentication flows.

3.  WebBridgeJS: The WebBridgeJS object acts as a communication channel between the native Android and JavaScript code running in the WebView. It enables bidirectional communication, allowing the native application to call JavaScript functions and vice versa.

4.  Customized Authentication Flows: Web Screen-Sets allow for customization of the authentication flow, enabling developers to tailor the user experience to their specific needs. This includes customizing the look and feel of the UI, adding custom logic, and integrating with other services.


**Benefits of Using Web Screen-Sets:**


*   **Flexibility and Customization:** Web Screen-Sets offer a high degree of flexibility and customization, allowing developers to create unique and engaging user experiences.

*   **Web Technologies:** Leverage the power of web technologies, such as HTML, CSS, and JavaScript, to build dynamic and interactive UIs.

*   **Seamless Integration:** WebBridgeJS provides a seamless integration between the native Android application and the web-based screen-sets.

*   **Reduced Development Effort**: Web Screen-Sets can simplify the development process by allowing developers to reuse existing web development skills and resources.


By utilizing Web Screen-Sets and WebBridgeJS, developers can create robust and customizable authentication flows for their Android applications, enhancing the user experience and streamlining the login and registration process.

## WebBridgeJS Usage in ScreenSetView Composable

The ScreenSetView composable function demonstrates the usage of the WebBridgeJS object to integrate and interact with web-based screen-sets within an Android application. Here's a breakdown of how it's used:


**Initialization and Configuration**


1.  Creating a WebBridgeJS instance:


    val webBridgeJS: WebBridgeJS = viewModel.newWebBridgeJS()

A new `WebBridgeJS` instance is created using the `newWebBridgeJS()` method from the `ViewModelScreenSet`. This instance will be used to manage the communication between the native code and the web screen-set.


2.  Adding configurations:


    webBridgeJS.addConfig(
            WebBridgeJSConfig.Builder().obfuscate(true).build()
        )

Configurations are added to the `WebBridgeJS` instance using the `addConfig()` method. In this case, obfuscation is enabled using `WebBridgeJSConfig.Builder().obfuscate(true).build()`.


**Attaching to WebView and Setting Authenticators**


1.  Attaching to WebView:


    webBridgeJS.attachBridgeTo(webView, viewModel.viewModelScope)

The `attachBridgeTo()` method is called to attach the `WebBridgeJS` instance to the `WebView` element. This establishes the bridge for communication. The `viewModelScope` is passed to manage the lifecycle of the bridge.


2.  Setting native social providers: (optional)


    webBridgeJS.setNativeSocialProviders(
            viewModel.identityService.getAuthenticatorMap()
        )

The `setNativeSocialProviders()` method is used to set the native social providers for authentication. The `AuthenticatorMap` from the `IdentityService` is passed to provide the necessary authenticators.


**Registering for Events and Loading Screen-sets**


1.  Registering for events:


    webBridgeJS.registerForEvents { webBridgeJSEvent ->
            // Handle events here
        }

The `registerForEvents()` method is used to register a callback function that will be invoked when events are triggered by the web screen-set. This allows the native code to respond to events from the web UI


2.  Loading the screen-set:


    webBridgeJS.load(webView, screenSetUrl)

The `load()` method is called to load the web screen-set into the `WebView`. The `screenSetUrl` specifies the URL of the screen-set to be loaded.


**Detaching from WebView**


    webBridgeJS.detachBridgeFrom(webView)

The `detachBridgeFrom()` method is called to detach the `WebBridgeJS` instance from the `WebView` when the composable is no longer in use. This cleans up the bridge and releases resources.


# Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports, etc. via \[GitHub issues\]([https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/issues](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/issues) ). Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project structure, and additional contribution information, see our \[Contribution Guidelines\]([CONTRIBUTING.md](http://CONTRIBUTING.md) ).


# Security / Disclosure

If you find any bug that may be a security problem, please follow our instructions at \[in our security policy\]([https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/security/policy](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/security/policy) ) on how to report it. Please do not create GitHub issues for security-related doubts or problems.


# Code of Conduct

As members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to always abide by its \[Code of Conduct\]([https://github.com/SAP/.github/blob/main/CODE\_OF\_CONDUCT.md](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md) ).


# Licensing

Copyright 2024 SAP SE or an SAP affiliate company and sap-customer-data-cloud-sdk-for-android contributors. Please see our \[LICENSE\](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available \[via the REUSE tool\]([https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android](https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android) ).