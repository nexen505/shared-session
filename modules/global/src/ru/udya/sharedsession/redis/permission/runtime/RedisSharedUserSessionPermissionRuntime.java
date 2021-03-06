package ru.udya.sharedsession.redis.permission.runtime;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionParentHelper;
import ru.udya.sharedsession.permission.runtime.SharedUserSessionPermissionRuntime;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserSessionPermissionRepository;
import ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component(SharedUserSessionPermissionRuntime.NAME)
public class RedisSharedUserSessionPermissionRuntime
        implements SharedUserSessionPermissionRuntime<RedisSharedUserSessionId, String> {

    protected SharedUserPermissionParentHelper permissionParentHelper;

    protected RedisSharedUserSessionRepository sessionRepository;
    protected RedisSharedUserSessionPermissionRepository sessionPermissionRepository;

    public RedisSharedUserSessionPermissionRuntime(
            SharedUserPermissionParentHelper permissionParentHelper,
            RedisSharedUserSessionRepository sessionRepository,
            RedisSharedUserSessionPermissionRepository sessionPermissionRepository) {

        this.permissionParentHelper = permissionParentHelper;
        this.sessionRepository = sessionRepository;
        this.sessionPermissionRepository = sessionPermissionRepository;
    }

    @Override
    public boolean isPermissionGrantedToUserSession(RedisSharedUserSessionId userSession,
                                                    SharedUserPermission permission) {

        // Redis implementation doesn't support so deep permissions
        if (permission instanceof SharedUserEntityAttributePermission
            || permission instanceof SharedUserScreenElementPermission) {

            return true;
        }

        var parentPermissions =
                permissionParentHelper.calculateParentPermissions(permission);

        var allPermissions = Stream.concat(Stream.of(permission),
                                        parentPermissions.stream())
                            .distinct().collect(toList());

        var isGranted = sessionPermissionRepository
                .doesHavePermissions(userSession, allPermissions);


        // if one of true then return true
        return isGranted.stream().filter(g -> g).findAny().orElse(false);
    }

    @Override
    public boolean isPermissionsGrantedToUserSession(RedisSharedUserSessionId userSession,
                                                     List<SharedUserPermission> permissions) {

        // Redis implementation doesn't support so deep permissions
        var supportedPermissions = permissions
                .stream().filter(p -> ! (
                        p instanceof SharedUserEntityAttributePermission
                        || p instanceof SharedUserScreenElementPermission));


        var parentPermissions = supportedPermissions
                .flatMap(p -> permissionParentHelper.calculateParentPermissions(p).stream())
                .distinct();

        var allPermissions = Stream.concat(permissions.stream(),
                                           parentPermissions)
                                .distinct().collect(toList());

        var isGranted = sessionPermissionRepository
                .doesHavePermissions(userSession, allPermissions);

        // if one of true then return true
        return isGranted.stream().filter(g -> g).findAny().orElse(false);
    }

    @Override
    public void grantPermissionToUserSession(RedisSharedUserSessionId userSession, SharedUserPermission permission) {
        sessionPermissionRepository.addToUserSession(userSession, permission);
    }

    @Override
    public void grantPermissionsToUserSession(RedisSharedUserSessionId userSession,
                                              List<? extends SharedUserPermission> permission) {
        sessionPermissionRepository.addToUserSession(userSession, permission);
    }

    @Override
    public void grantPermissionToUserSessions(List<? extends RedisSharedUserSessionId> userSessions,
                                              SharedUserPermission permission) {

        for (var userSession : userSessions) {
            sessionPermissionRepository.addToUserSession(userSession, permission);
        }
    }

    @Override
    public void grantPermissionsToUserSessions(List<? extends RedisSharedUserSessionId> userSessions,
                                               List<? extends SharedUserPermission> permissions) {

        for (var userSession : userSessions) {
            sessionPermissionRepository.addToUserSession(userSession, permissions);
        }
    }

    @Override
    public void grantPermissionToAllUserSessions(Id<User, UUID> userId, SharedUserPermission permission) {
        var userSessionsIds = sessionRepository.findAllIdsByUser(userId);

        for (var userSessionId : userSessionsIds) {
            sessionPermissionRepository.addToUserSession(userSessionId, permission);
        }
    }

    @Override
    public void grantPermissionsToAllUserSessions(Id<User, UUID> userId,
                                                  List<? extends SharedUserPermission> permissions) {

        var userSessionsIds = sessionRepository.findAllIdsByUser(userId);

        for (var userSessionId : userSessionsIds) {
            sessionPermissionRepository.addToUserSession(userSessionId, permissions);
        }
    }

    @Override
    public void grantPermissionToAllUsersSessions(Ids<User, UUID> userIds,
                                                  SharedUserPermission permission) {

        userIds.getValues().stream()
               .flatMap(uId -> sessionRepository.findAllIdsByUser(Id.of(uId, User.class)).stream())
               .forEach(s -> sessionPermissionRepository.addToUserSession(s, permission));
    }

    @Override
    public void grantPermissionsToAllUsersSessions(Ids<User, UUID> userIds,
                                                   List<? extends SharedUserPermission> permissions) {

        userIds.getValues().stream()
               .flatMap(uId -> sessionRepository.findAllIdsByUser(Id.of(uId, User.class)).stream())
               .forEach(s -> sessionPermissionRepository.addToUserSession(s, permissions));
    }

    @Override
    public void revokePermissionFromUserSession(RedisSharedUserSessionId userSession,
                                                SharedUserPermission permission) {

        sessionPermissionRepository.removeFromUserSession(userSession, permission);

    }

    @Override
    public void revokePermissionsFromUserSession(RedisSharedUserSessionId userSession,
                                                 List<? extends SharedUserPermission> permission) {
        sessionPermissionRepository.removeFromUserSession(userSession, permission);
    }

    @Override
    public void revokePermissionFromUserSessions(List<? extends RedisSharedUserSessionId> userSessions,
                                                 SharedUserPermission permission) {

        for (var userSession : userSessions) {
            sessionPermissionRepository.removeFromUserSession(userSession, permission);
        }
    }

    @Override
    public void revokePermissionsFromUserSessions(List<? extends RedisSharedUserSessionId> userSessions,
                                                  List<? extends SharedUserPermission> permissions) {

        for (var userSession : userSessions) {
            sessionPermissionRepository.removeFromUserSession(userSession, permissions);
        }
    }

    @Override
    public void revokePermissionFromAllUserSessions(Id<User, UUID> userId, SharedUserPermission permission) {
        var userSessionsIds = sessionRepository.findAllIdsByUser(userId);

        for (var userSessionId : userSessionsIds) {
            sessionPermissionRepository.removeFromUserSession(userSessionId, permission);
        }
    }

    @Override
    public void revokePermissionsFromAllUserSessions(Id<User, UUID> userId,
                                                     List<? extends SharedUserPermission> permissions) {

        var userSessionsIds = sessionRepository.findAllIdsByUser(userId);

        for (var userSessionId : userSessionsIds) {
            sessionPermissionRepository.addToUserSession(userSessionId, permissions);
        }
    }

    @Override
    public void revokePermissionFromAllUsersSessions(Ids<User, UUID> userIds,
                                                     SharedUserPermission permission) {

        userIds.getValues().stream()
               .flatMap(uId -> sessionRepository.findAllIdsByUser(Id.of(uId, User.class)).stream())
               .forEach(s -> sessionPermissionRepository.removeFromUserSession(s, permission));

    }

    @Override
    public void revokePermissionsFromAllUsersSessions(Ids<User, UUID> userIds,
                                                      List<? extends SharedUserPermission> permissions) {

        userIds.getValues().stream()
               .flatMap(uId -> sessionRepository.findAllIdsByUser(Id.of(uId, User.class)).stream())
               .forEach(s -> sessionPermissionRepository.removeFromUserSession(s, permissions));
    }
}
