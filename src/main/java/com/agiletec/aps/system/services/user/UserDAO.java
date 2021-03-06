/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.agiletec.aps.system.services.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.common.AbstractDAO;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Data Access Object per gli oggetti Utente.
 *
 * @author M.Diana - E.Santoboni
 */
public class UserDAO extends AbstractDAO implements IUserDAO {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(UserDAO.class);

    private PasswordEncoder passwordEncoder;

    @Override
    public List<String> loadUsernames() {
        return this.searchUsernames(null);
    }

    @Override
    public List<String> searchUsernames(String text) {
        if (null != text && text.trim().length() == 0) {
            text = null;
        }
        Connection conn = null;
        List<String> usernames = new ArrayList<>();
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            conn = this.getConnection();
            if (text == null) {
                stat = conn.prepareStatement(LOAD_USERNAMES);           //NOSONAR
            } else {
                stat = conn.prepareStatement(SEARCH_USERNAMES_BY_TEXT); //NOSONAR
                stat.setString(1, "%" + text + "%");
            }
            res = stat.executeQuery();
            while (res.next()) {
                usernames.add(res.getString(1));
            }
        } catch (Throwable t) {
            logger.error("Error loading the usernames list", t);
            throw new RuntimeException("Error loading the usernames list", t);
        } finally {
            closeDaoResources(res, stat, conn);
        }
        return usernames;
    }

    @Override
    public List<UserDetails> loadUsers() {
        return this.searchUsers(null);
    }

    @Override
    public List<UserDetails> searchUsers(String text) {
        if (null != text && text.trim().length() == 0) {
            text = null;
        }
        Connection conn = null;
        List<UserDetails> users = null;
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            conn = this.getConnection();
            if (text == null) {
                stat = conn.prepareStatement(LOAD_USERS);           //NOSONAR
            } else {
                stat = conn.prepareStatement(SEARCH_USERS_BY_TEXT); //NOSONAR
                stat.setString(1, "%" + text + "%");
            }
            res = stat.executeQuery();
            users = this.loadUsers(res);
        } catch (Throwable t) {
            logger.error("Error while searching users  by '{}' ", text, t);
            throw new RuntimeException("Error while searching users", t);
        } finally {
            closeDaoResources(res, stat, conn);
        }
        return users;
    }

    protected List<UserDetails> loadUsers(ResultSet result) throws SQLException {
        List<UserDetails> users = new ArrayList<>();
        while (result.next()) {
            String userName = result.getString(1);
            User user = new User();
            user.setUsername(userName);
            user.setPassword(result.getString(2));
            user.setCreationDate(result.getDate(3));
            user.setLastAccess(result.getDate(4));
            user.setLastPasswordChange(result.getDate(5));
            int activeId = result.getInt(6);
            user.setDisabled(activeId != 1);
            users.add(user);
        }
        return users;
    }

    /**
     * Carica un'utente corrispondente alla userName e password immessa. null se
     * non vi ?? nessun utente corrispondente.
     *
     * @param username Nome utente dell'utente cercato
     * @param password password dell'utente cercato
     * @return L'oggetto utente corrispondente ai parametri richiesti, oppure
     * null se non vi ?? nessun utente corrispondente.
     */
    @Override
    public UserDetails loadUser(String username, String password) {
        try {
            UserDetails user = this.loadUser(username);
            if (user != null && passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        } catch (Throwable t) {
            logger.error("Error while loading the user {} ", username, t);
            throw new RuntimeException("Error while loading the user " + username, t);
        }
        return null;
    }

    /**
     * Carica un'utente corrispondente alla userName immessa. null se non vi ??
     * nessun utente corrispondente.
     *
     * @param username Nome utente dell'utente cercato.
     * @return L'oggetto utente corrispondente ai parametri richiesti, oppure
     * null se non vi ?? nessun utente corrispondente.
     */
    @Override
    public UserDetails loadUser(String username) {
        Connection conn = null;
        User user = null;
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            conn = this.getConnection();
            stat = conn.prepareStatement(LOAD_USER_FROM_USERNAME);      //NOSONAR
            stat.setString(1, username);
            res = stat.executeQuery();
            user = this.createUserFromRecord(res);
        } catch (Throwable t) {
            logger.error("Error while loading the user {}", username, t);
            throw new RuntimeException("Error while loading the user " + username, t);
        } finally {
            closeDaoResources(res, stat, conn);
        }
        return user;
    }

    /**
     * Cancella l'utente.
     *
     * @param user L'oggetto di tipo User relativo all'utente da cancellare.
     */
    @Override
    public void deleteUser(UserDetails user) {
        this.deleteUser(user.getUsername());
    }

    /**
     * Cancella l'utente corrispondente alla userName immessa.
     *
     * @param username Il nome identificatore dell'utente.
     */
    @Override
    public void deleteUser(String username) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            //this.removeUserGroups(username, conn);
            //this.removeUserRoles(username, conn);
            stat = conn.prepareStatement(DELETE_USER);
            stat.setString(1, username);
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            logger.error("Error while deleting the user {}", username, t);
            throw new RuntimeException("Error while deleting the user " + username, t);
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }

    /**
     * Aggiunge un nuovo utente.
     *
     * @param user Oggetto di tipo User relativo all'utente da aggiungere.
     */
    @Override
    public void addUser(UserDetails user) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(ADD_USER);
            stat.setString(1, user.getUsername());
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            stat.setString(2, encodedPassword);
            stat.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
            if (!user.isDisabled()) {
                stat.setInt(4, 1);
            } else {
                stat.setInt(4, 0);
            }
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            logger.error("Error while adding a new user {}", user, t);
            throw new RuntimeException("Error while adding a new user", t);
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }

    /**
     * Aggiorna un utente gi?? presente con nuovi valori (tranne la username che
     * ?? fissa).
     *
     * @param user Oggetto di tipo User relativo all'utente da aggiornare.
     */
    @Override
    public void updateUser(UserDetails user) {
        User entandoUser = ((user instanceof User) ? (User) user : null);
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(UPDATE_USER);
            if (null != entandoUser && null != entandoUser.getLastAccess()) {
                stat.setDate(1, new java.sql.Date(entandoUser.getLastAccess().getTime()));
            } else {
                stat.setNull(1, Types.DATE);
            }
            if (null != entandoUser && null != entandoUser.getLastPasswordChange()) {
                stat.setDate(2, new java.sql.Date(entandoUser.getLastPasswordChange().getTime()));
            } else {
                stat.setNull(2, Types.DATE);
            }
            if (null != entandoUser) {
                if (!entandoUser.isDisabled()) {
                    stat.setInt(3, 1);
                } else {
                    stat.setInt(3, 0);
                }
            } else {
                stat.setNull(3, Types.NUMERIC);
            }
            stat.setString(4, user.getUsername());
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            logger.error("Error while updating the user {}", user.getUsername(), t);
            throw new RuntimeException("Error while adding a new user", t);
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }

    @Override
    public void changePassword(String username, String password) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(CHANGE_PASSWORD);
            String encodedPassowrd = passwordEncoder.encode(password);
            stat.setString(1, encodedPassowrd);
            stat.setDate(2, new java.sql.Date(new Date().getTime()));
            stat.setString(3, username);
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            logger.error("Error updating the password for the user {}", username, t);
            throw new RuntimeException("Error updating the password for the user " + username, t);
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }

    @Override
    public void updateLastAccess(String username) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(UPDATE_LAST_ACCESS);
            stat.setDate(1, new java.sql.Date(new java.util.Date().getTime()));
            stat.setString(2, username);
            stat.executeUpdate();
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            logger.error("Error updating the last access for the user {}", username, t);
            throw new RuntimeException("Error updating the last access for the user " + username, t);
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }

    /**
     * Crea un utente leggendo i valori dal record corrente del ResultSet
     * passato. Attenzione: la query di origine del ResultSet deve avere nella
     * select list i campi esattamente in questo numero e ordine: 1=username,
     * 2=passwd
     *
     * @param res Il ResultSet da cui leggere i valori
     * @return L'oggetto user popolato.
     * @throws SQLException
     */
    protected User createUserFromRecord(ResultSet res) throws SQLException {
        User user = null;
        if (res.next()) {
            user = new User();
            user.setUsername(res.getString(1));
            user.setPassword(res.getString(2));
            user.setCreationDate(res.getDate(3));
            user.setLastAccess(res.getDate(4));
            user.setLastPasswordChange(res.getDate(5));
            int activeId = res.getInt(6);
            user.setDisabled(activeId != 1);
        }
        return user;
    }

    @Override
    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public void setPasswordEncoder(PasswordEncoder encoder) {
        this.passwordEncoder = encoder;
    }

    private final String PREFIX_LOAD_USERNAMES
            = "SELECT authusers.username FROM authusers ";

    private final String PREFIX_LOAD_USERS
            = "SELECT authusers.username, authusers.passwd, authusers.registrationdate, "
            + "authusers.lastaccess, authusers.lastpasswordchange, authusers.active FROM authusers ";

    private final String LOAD_USERNAMES
            = PREFIX_LOAD_USERNAMES + "ORDER BY authusers.username";

    private final String LOAD_USERS
            = PREFIX_LOAD_USERS + "ORDER BY authusers.username";

    private final String SEARCH_USERNAMES_BY_TEXT
            = PREFIX_LOAD_USERNAMES + " WHERE authusers.username LIKE ? ORDER BY authusers.username";

    private final String SEARCH_USERS_BY_TEXT
            = PREFIX_LOAD_USERS + " WHERE authusers.username LIKE ? ORDER BY authusers.username";

    private final String LOAD_USER_FROM_USERNAME
            = PREFIX_LOAD_USERS + "WHERE authusers.username = ? ";

    private final String DELETE_USER
            = "DELETE FROM authusers WHERE username = ? ";

    private final String ADD_USER
            = "INSERT INTO authusers (username, passwd, registrationdate, active) VALUES ( ? , ? , ? , ? )";

    private final String CHANGE_PASSWORD
            = "UPDATE authusers SET passwd = ? , lastpasswordchange = ? WHERE username = ? ";

    private final String UPDATE_USER
            = "UPDATE authusers SET lastaccess = ? , lastpasswordchange = ? , active = ? WHERE username = ? ";

    private final String UPDATE_LAST_ACCESS
            = "UPDATE authusers SET lastaccess = ? WHERE username = ? ";

}
