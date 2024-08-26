package com.sql.parser;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Myron
 * @since 2022/6/24 9:48
 */
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JsqlTree {

    private String name;
    private List<JsqlTree> children=new ArrayList<>();
}
