package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.OneToMany

@MappedSuperclass
interface RoleBase : NamedEntity {

    @OneToMany(mappedBy = "role")
    val permissions: List<Permission>
}