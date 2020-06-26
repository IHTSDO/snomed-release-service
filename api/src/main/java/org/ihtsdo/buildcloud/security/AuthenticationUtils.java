package org.ihtsdo.buildcloud.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.HashSet;
import java.util.Set;

/**
 * User: huyle
 * Date: 2/21/2015
 * Time: 11:10 AM
 */
public class AuthenticationUtils {

   public static UserDetails getUserDetails() {
      final SecurityContext context = SecurityContextHolder.getContext();
      if (context != null) {
         final Authentication authentication = context.getAuthentication();
         if (authentication != null) {
            return (UserDetails) authentication.getPrincipal();
         }
      }
      return null;
   }

   private static Authentication getAuthentication() {
      final SecurityContext context = SecurityContextHolder.getContext();
      if (context != null) {
         return context.getAuthentication();
      }
      return null;
   }

   public static String getCurrentUserName() {
      Authentication authentication = getAuthentication();
      if(authentication != null) {
         if(authentication instanceof PreAuthenticatedAuthenticationToken) {
            return authentication.getPrincipal() != null ? authentication.getPrincipal().toString() : null;
         } else {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails != null ? userDetails.getUsername() : null;
         }
      }
      return null;
   }

   public static boolean currentUserHasRole(String role) {
      final Authentication authentication = getAuthentication();
      for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
         if(grantedAuthority.getAuthority().equalsIgnoreCase(role)) return true;
      }
      return false;
   }

   public static Set<String> getCurrentUserRoles() {
      final Authentication authentication = getAuthentication();
      Set<String> roles = new HashSet<>();
      for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
         roles.add(grantedAuthority.getAuthority());
      }
      return roles;
   }


}
