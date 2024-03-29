/*
    Security Filters with changes for CollabNet.  
    Based on SecurityFilters.groovy.
    Defines a part of the security configuration of Jenkins.

    This file must define a servlet Filter instance with the name 'filter'
*/
import hudson.plugins.collabnet.auth.CNAuthenticationEntryPoint;
import hudson.security.AccessDeniedHandlerImpl
import hudson.security.AuthenticationProcessingFilter2
import hudson.security.ChainedServletFilter
import hudson.security.UnwrapSecurityExceptionFilter
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter

import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter
import hudson.security.HttpSessionContextIntegrationFilter2
import org.springframework.security.web.access.ExceptionTranslationFilter


// providers that apply to both patterns
def commonProviders(redirectUrl) {
    return [
        bean(AnonymousAuthenticationFilter) {
            key = "anonymous" // must match with the AnonymousProvider
            userAttribute = "anonymous,"
        },
        bean(ExceptionTranslationFilter) {
            accessDeniedHandler = new AccessDeniedHandlerImpl()
            authenticationEntryPoint = bean(CNAuthenticationEntryPoint) {
                loginFormUrl = redirectUrl;
            }
        },
        bean(UnwrapSecurityExceptionFilter)
    ]
}

filter(ChainedServletFilter) {
    filters = [
        // this persists the authentication across requests by using session
        bean(HttpSessionContextIntegrationFilter2) {
        },
        // allow clients to submit basic authentication credential
        bean(BasicAuthenticationFilter) {
            authenticationManager = securityComponents.manager
            // if basic authentication fails (which only happens incorrect basic auth credential is sent),
            // respond with 401 with basic auth request, instead of redirecting the user to the login page,
            // since users of basic auth tends to be a program and won't see the redirection to the form
            // page as a failure
            authenticationEntryPoint = bean(BasicAuthenticationEntryPoint) {
                realmName = "Jenkins"
            }
        },
        bean(AuthenticationProcessingFilter2) {
            authenticationManager = securityComponents.manager
            rememberMeServices = securityComponents.rememberMe
            authenticationFailureUrl = "/loginError"
            defaultTargetUrl = "/"
            filterProcessesUrl = "/j_acegi_security_check"
        },
        bean(RememberMeAuthenticationFilter) {
            rememberMeServices = securityComponents.rememberMe
            authenticationManager = securityComponents.manager
        },
    ] + commonProviders("/login?from={0}")
}

// this filter set up is used to emulate the legacy Jenkins behavior
// of container authentication before 1.160 
legacy(ChainedServletFilter) {
    filters = [
        bean(BasicAuthenticationFilter)
    ] + commonProviders("/loginEntry?from={0}")
    // when using container-authentication we can't hit /login directly.
    // we first have to hit protected /loginEntry, then let the container
    // trap that into /login.
}
