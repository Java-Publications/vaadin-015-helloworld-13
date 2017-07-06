package org.rapidpm.vaadin.trainer.modules.login;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.rapidpm.vaadin.trainer.api.LoginService;

/**
 *
 */
public class LoginServiceShiroSimple implements LoginService {


  @Override
  public boolean check(String login, String password) {

    final Subject currentUser = SecurityUtils.getSubject();
    final UsernamePasswordToken token = new UsernamePasswordToken(login, password);

    try {
      currentUser.login(token);
      token.setRememberMe(true);

      return true;
    } catch (AuthenticationException e) {
      e.printStackTrace();

      token.setRememberMe(false);
      currentUser.logout();

      return false;
    }
  }


}
