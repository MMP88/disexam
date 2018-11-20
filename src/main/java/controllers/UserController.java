package controllers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import model.User;
import utils.Hashing;
import utils.Log;

public class UserController {

  private static DatabaseController dbCon;

  public UserController() {
    dbCon = new DatabaseController();
  }

  public static User getUser(int id) {

    // Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the query for DB
    String sql = "SELECT * FROM user where id=" + id;

    // Actually do the query
    ResultSet rs = dbCon.query(sql);
    User user = null;

    try {
      // Get first object, since we only have one
      if (rs.next()) {
        user =
                new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password"),
                        rs.getString("email"));

        // return the create object
        return user;
      } else {
        System.out.println("No user found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return null
    return user;
  }

  public static String getLogin(User user) {

    // Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the query for DB
    String sql = "SELECT * FROM user where email= '" + user.getEmail() + "' AND (password= '" + Hashing.shaSalt(user.getPassword() + "' OR password= '") + user.getPassword() + "')";

    // Actually do the query
    ResultSet rs = dbCon.query(sql);
    User userLogin;
    String token = null;

    try {
      // Get first object, since we only have one
      if (rs.next()) {
        userLogin =
                new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password"),
                        rs.getString("email"));
        if (userLogin != null) {
          try {
            Date expire = new Date();
            expire.setTime(System.currentTimeMillis() + 10000);
            Algorithm algorithm = Algorithm.HMAC256("secret");
            token = JWT.create()
                    .withClaim("userID", user.getId())
                    .withExpiresAt(expire)
                    .withIssuer("auth0")
                    .sign(algorithm);
          } catch (JWTCreationException e) {
            System.out.println(e.getMessage());
          } finally {
            return token;
          }
        }
      } else {
        System.out.println("No user found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return null
    return "";
  }

  /**
   * Get all users in database
   *
   * @return
   */
  public static ArrayList<User> getUsers() {

    // Check for DB connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build SQL
    String sql = "SELECT * FROM user";

    // Do the query and initialyze an empty list for use if we don't get results
    ResultSet rs = dbCon.query(sql);
    ArrayList<User> users = new ArrayList<User>();

    try {
      // Loop through DB Data
      while (rs.next()) {
        User user =
                new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password"),
                        rs.getString("email"));

        // Add element to list
        users.add(user);
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return the list of users
    return users;
  }

  public static User createUser(User user) {

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Actually creating a user in DB", 0);

    // Set creation time for user.
    user.setCreatedTime(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Insert the user in the DB
    // TODO: Hash the user password before saving it. :FIXED
    int userID = dbCon.insert(
            "INSERT INTO user(first_name, last_name, password, email, created_at) VALUES('"
                    + user.getFirstname()
                    + "', '"
                    + user.getLastname()
                    + "', '"
                    + Hashing.shaSalt(user.getPassword())
                    + "', '"
                    + user.getEmail()
                    + "', "
                    + user.getCreatedTime()
                    + ")");

    if (userID != 0) {
      //Update the userid of the user before returning
      user.setId(userID);
    } else {
      // Return null if user has not been inserted into database
      return null;
    }

    // Return user
    return user;
  }

  public static void delete(User user) {
    //Log.writeLog(UserController.class.getName(), idUser, "Bruger slettes",0);

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {
      String sqlStatement = "DELETE FROM user WHERE id =" + user.getId();
      dbCon.deleteUpdate(sqlStatement);

    } catch (JWTDecodeException e) {
      //Output
    }

  }

  public static User update(User user) {

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {

      PreparedStatement sqlStatement = dbCon.getConnection().prepareStatement(
              "UPDATE user SET first_name = ?, last_name = ? WHERE id = ?");

      // set the preparedstatement parameters
      sqlStatement.setString(1,user.getFirstname());
      sqlStatement.setString(2,user.getLastname());
      sqlStatement.setInt(3,user.getId());

      sqlStatement.executeUpdate();

    } catch (SQLException e) {
      //Output
    }
    return user;
  }

  public static String getTokenVerification(User user) {

      // Check for connection
      if (dbCon == null) {
        dbCon = new DatabaseController();
      }

      // Build the query for DB
      String sql = "SELECT * FROM user where id=" + user.getId();

      // Actually do the query
      ResultSet rs = dbCon.query(sql);
      User sessionToken;
      String token = user.getToken();

      try {
        // Get first object, since we only have one
        if (rs.next()) {
          sessionToken =
                  new User(
                          rs.getInt("id"),
                          rs.getString("first_name"),
                          rs.getString("last_name"),
                          rs.getString("password"),
                          rs.getString("email"));
          if (sessionToken != null) {
            try {
              Algorithm algorithm = Algorithm.HMAC256("secret");
              JWTVerifier verifier = JWT.require(algorithm)
                      .withIssuer("auth0")
                      .build();
              DecodedJWT jwt = verifier.verify(token);
              Claim claim = jwt.getClaim("userID");

              if (user.getId() == claim.asInt()) {
                return token;
              }
            } catch (JWTCreationException e) {
              System.out.println(e.getMessage());
            }
          }
        } else {
          System.out.println("No user found");
        }
      } catch (SQLException ex) {
        System.out.println(ex.getMessage());
      }

      // Return null
      return "";
    }

}