package org.terasology.worldviewer.env;

import java.security.Permission;

import org.terasology.module.Module;
import org.terasology.module.sandbox.PermissionProvider;
import org.terasology.module.sandbox.PermissionProviderFactory;

public class DummyPermissionProviderFactory implements PermissionProviderFactory {

    @Override
    public PermissionProvider createPermissionProviderFor(Module module) {
        return new PermissionProvider() {

            @Override
            public boolean isPermitted(Permission permission, Class<?> context) {
                return true;
            }

            @Override
            public boolean isPermitted(Class type) {
                return true;
            }
        };
    }

}
