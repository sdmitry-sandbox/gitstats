package io.cx.gitstats.web.mapper;

import io.cx.gitstats.domain.model.Project;
import io.cx.gitstats.web.model.ProjectWebModel;
import org.mapstruct.Mapper;

@Mapper
public interface WebMapper {

    ProjectWebModel toWebModel(Project domain);
}
