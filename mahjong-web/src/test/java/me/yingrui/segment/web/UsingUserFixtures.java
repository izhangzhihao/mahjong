package me.yingrui.segment.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import me.yingrui.segment.web.ui.model.User;
import me.yingrui.segment.web.ui.service.UserService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsingUserFixtures extends UsingFixtures {

    @Autowired
    private UserService userService;

    protected void setUpCurrentUser(String currentUserEmail) {
        User user = new User();
        user.setEmail(currentUserEmail);
        userService.addUser(user);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(currentUserEmail);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
