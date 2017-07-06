package org.rapidpm.vaadin.trainer.modules.login;

import static java.lang.System.out;
import static java.time.Duration.between;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.rapidpm.frp.model.Pair;
import org.rapidpm.vaadin.trainer.api.LoginService;

/**
 *
 */
public class LoginServiceShiro implements LoginService {

  private static final Map<String, Pair<LocalDateTime, Integer>> failedLogins = new ConcurrentHashMap<>();
  public static final int MAX_FAILED_LOGINS = 3;
  public static final int MINUTES_TO_WAIT = 1;
  public static final int MINUTES_TO_BE_CLEANED = 2;
  public static final int MILLISECONDS_TO_BE_CLEANED = 1_000 * 60 * MINUTES_TO_BE_CLEANED;

  public static final int MILLISECONDS_INITIAL_DELAY = 100;


  public static class FailedLoginCleaner {
    private final Timer failedLoginCleanUpTimer = new Timer();

    public FailedLoginCleaner(TimerTask tasknew) {
      failedLoginCleanUpTimer.schedule(tasknew, MILLISECONDS_INITIAL_DELAY, MILLISECONDS_TO_BE_CLEANED);
    }
  }

  private static final FailedLoginCleaner FAILED_LOGIN_CLEANER = new FailedLoginCleaner(new TimerTask() {
    @Override
    public void run() {
      out.println(" start cleaning " + LocalDateTime.now());
      failedLogins
          .keySet()
          .forEach((String key) -> {
            Pair<LocalDateTime, Integer> pair = failedLogins.get(key);
            if (pair != null) {
              out.println("work on login/pair = " + key + " - " + pair);
              final Duration duration = between(pair.getT1(), LocalDateTime.now());
              long minutes = duration.toMinutes();
              if (minutes > MINUTES_TO_BE_CLEANED) {
                failedLogins.remove(key); // start from zero
                out.println("  ==>  cleaned key = " + key);
              }
            }
          });
    }
  });


  @Override
  public boolean check(String login, String password) {
    //TODO FAILED LOGIN Counter Rule
    if (failedLogins.containsKey(login)) {
      Pair<LocalDateTime, Integer> pair = failedLogins.get(login);
      LocalDateTime failedLoginDate = pair.getT1();
      Integer failedLoginCount = pair.getT2();
      if (failedLoginCount > MAX_FAILED_LOGINS) {
        out.println("failedLoginCount > MAX_FAILED_LOGINS " + failedLoginCount);
        final Duration duration = between(failedLoginDate, LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes > MINUTES_TO_WAIT) {
          out.println("minutes > MINUTES_TO_WAIT (remove login) " + failedLoginCount);
          failedLogins.remove(login); // start from zero
        }
        else {
          out.println("failedLoginCount <= MAX_FAILED_LOGINS " + failedLoginCount);
          failedLogins.compute(
              login,
              (s, faildPair) -> new Pair<>(LocalDateTime.now(), failedLoginCount + 1));
          return false;
        }
      }
      else {
        out.println("failedLoginCount => " + login + " - " + failedLoginCount);
      }
    }

    final UsernamePasswordToken token = new UsernamePasswordToken(login, password);
    final Subject subject = SecurityUtils.getSubject();
    try {
      subject.login(token);
      failedLogins.remove(login);
    } catch (AuthenticationException e) {
      out.println("login failed " + login);
      //e.printStackTrace();
      failedLogins.putIfAbsent(login, new Pair<>(LocalDateTime.now(), 0));
      failedLogins.compute(
          login,
          (s, oldPair) -> new Pair<>(LocalDateTime.now(), oldPair.getT2() + 1));
    }
    return subject.isAuthenticated();
  }





}
