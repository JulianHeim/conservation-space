/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.PersistentObject;
import org.activiti.engine.impl.persistence.AbstractManager;


// TODO: Auto-generated Javadoc
/**
 * The Class UserManager.
 *
 * @author Tom Baeyens
 * @author Saeid Mirzaei
 */
public class UserManager extends AbstractManager {

  /**
   * Creates the new user.
   *
   * @param userId the user id
   * @return the user
   */
  public User createNewUser(String userId) {
    return new UserEntity(userId);
  }

  /**
   * Insert user.
   *
   * @param user the user
   */
  public void insertUser(User user) {
    getDbSqlSession().insert((PersistentObject) user);
  }
  
  /**
   * Update user.
   *
   * @param updatedUser the updated user
   */
  public void updateUser(User updatedUser) {
    UserEntity persistentUser = findUserById(updatedUser.getId());
    persistentUser.update((UserEntity) updatedUser);
  }

  /**
   * Find user by id.
   *
   * @param userId the user id
   * @return the user entity
   */
  public UserEntity findUserById(String userId) {
    return (UserEntity) getDbSqlSession().selectOne("selectUserById", userId);
  }

  /**
   * Delete user.
   *
   * @param userId the user id
   */
  @SuppressWarnings("unchecked")
  public void deleteUser(String userId) {
    UserEntity user = findUserById(userId);
    if (user!=null) {
      if (user.getPictureByteArrayId()!=null) {
        getDbSqlSession().delete(ByteArrayEntity.class, user.getPictureByteArrayId());
      }
      List<IdentityInfoEntity> identityInfos = getDbSqlSession().selectList("selectIdentityInfoByUserId", userId);
      for (IdentityInfoEntity identityInfo: identityInfos) {
        getIdentityInfoManager().deleteIdentityInfo(identityInfo);
      }
      getDbSqlSession().delete("deleteMembershipsByUserId", userId);
      getDbSqlSession().delete("deleteUser", userId);
    }
  }
  
  /**
   * Find user by query criteria.
   *
   * @param query the query
   * @param page the page
   * @return the list
   */
  @SuppressWarnings("unchecked")
  public List<User> findUserByQueryCriteria(UserQueryImpl query, Page page) {
    return getDbSqlSession().selectList("selectUserByQueryCriteria", query, page);
  }
  
  /**
   * Find user count by query criteria.
   *
   * @param query the query
   * @return the long
   */
  public long findUserCountByQueryCriteria(UserQueryImpl query) {
    return (Long) getDbSqlSession().selectOne("selectUserCountByQueryCriteria", query);
  }
  
  /**
   * Find groups by user.
   *
   * @param userId the user id
   * @return the list
   */
  @SuppressWarnings("unchecked")
  public List<Group> findGroupsByUser(String userId) {
    return getDbSqlSession().selectList("selectGroupsByUserId", userId);
  }

  /**
   * Creates the new user query.
   *
   * @return the user query
   */
  public UserQuery createNewUserQuery() {
    return new UserQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutorTxRequired());
  }

  /**
   * Find user info by user id and key.
   *
   * @param userId the user id
   * @param key the key
   * @return the identity info entity
   */
  public IdentityInfoEntity findUserInfoByUserIdAndKey(String userId, String key) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("userId", userId);
    parameters.put("key", key);
    return (IdentityInfoEntity) getDbSqlSession().selectOne("selectIdentityInfoByUserIdAndKey", parameters);
  }

  /**
   * Find user info keys by user id and type.
   *
   * @param userId the user id
   * @param type the type
   * @return the list
   */
  @SuppressWarnings("unchecked")
  public List<String> findUserInfoKeysByUserIdAndType(String userId, String type) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("userId", userId);
    parameters.put("type", type);
    return (List) getDbSqlSession().getSqlSession().selectList("selectIdentityInfoKeysByUserIdAndType", parameters);
  }
  
  /**
   * Check password.
   *
   * @param userId the user id
   * @param password the password
   * @return the boolean
   */
  public Boolean checkPassword(String userId, String password) {
    User user = findUserById(userId);
    if ((user != null) && (password != null) && (password.equals(user.getPassword()))) {
      return true;
    }
    return false;
  }
  
  /**
   * Find potential starter users.
   *
   * @param proceDefId the proce def id
   * @return the list
   */
  @SuppressWarnings("unchecked")
  public List<User> findPotentialStarterUsers(String proceDefId) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("procDefId", proceDefId);
    return  (List<User>) getDbSqlSession().selectOne("selectUserByQueryCriteria", parameters);
    
  }
  
  
}