package com.sql.parser;


import com.sql.constant.TreeNodePosition;
import com.sql.entity.ExpressionAttr;
import com.sql.entity.Param;
import com.sql.utils.ParamUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

import java.util.*;

/**
 * @author Myron
 * @since 2022/6/10 10:14
 */
@Slf4j
public class MyExpressionDeParser extends ExpressionDeParser {

    /**
     * 参数名称以及对应的值
     */
    private Map<String, Object> params;

    public Map<String, SqlCondition> paramExpression = new LinkedHashMap<>();

    /**
     * 存储子Expression与父Expression的映射
     */
    public Map<Expression, Expression> childParentExpressionMap = new LinkedHashMap<>();

    /**
     * 移除表达式集合
     */
    public HashSet<Expression> removeExpressionSet = new LinkedHashSet<>();

    /**
     * 表示Expression 是为左节点还是有节点
     */
    public Map<Expression, ExpressionAttr> expressionAttrMap = new LinkedHashMap<>();

    public SelectBody selectBody;

    public MyExpressionDeParser(Map<String, Object> params) {
        this.params = params;
    }

    private JsqlTree jsqlTree;

    /**
     * 将SQL语句中不存在值的查询条件剔除（参数名称表达式为${name}，params.get(name)作为是否有值的判断依据）
     *
     * @param select
     */
    public void dealSelect(Select select) {
        if (select != null) {
            SelectBody selectBody = select.getSelectBody();
            if (this.selectBody == null) {
                this.selectBody = selectBody;
            }
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                this.dealPlainSelect(plainSelect);
            } else if (selectBody instanceof SetOperationList) {
                SetOperationList operationList = (SetOperationList) selectBody;
                this.dealSetOperationList(operationList);
            }


            //Oracle得的WITH 语句
            List<WithItem> withItemsList = select.getWithItemsList();
            if (withItemsList != null) {
                for (WithItem item : withItemsList) {
                    SubSelect subSelect = item.getSubSelect();
                    this.dealSubSelect(subSelect);
                }
            }
        }
    }

    private void dealPlainSelect(PlainSelect plainSelect) {
        if (plainSelect != null) {
            List<SelectItem> selectItems = plainSelect.getSelectItems();
            List<Join> joins = plainSelect.getJoins();
            FromItem fromItem = plainSelect.getFromItem();
            Expression expression = plainSelect.getWhere();

            //处理select的列
            this.dealSelectItem(selectItems);
            //处理join
            this.dealJoins(joins);
            //处理from子查询
            this.dealFromItem(fromItem);
            //处理where条件
            this.dealBinaryExpression(expression, null, TreeNodePosition.ROOT);

            this.visit(plainSelect.getOracleHierarchical());
        }
    }

    /**
     * new MyExpressionDeParser并处理PlainSelect
     *
     * @param plainSelect
     */
    private MyExpressionDeParser newExpressionDeParserAndDealPlainSelect(PlainSelect plainSelect) {
        //改变二叉树节点
        MyExpressionDeParser expressionDeParser = new MyExpressionDeParser(this.params);
        //使用原MyExpressionDeParser实例对象的属性，保证子查询的参数可以被原MyExpressionDeParser实例对象获取到
        expressionDeParser.childParentExpressionMap = this.childParentExpressionMap;
        expressionDeParser.paramExpression = this.paramExpression;
        //新表达式解析类的SQL必须变更
        expressionDeParser.selectBody = plainSelect;

        expressionDeParser.dealPlainSelect(plainSelect);
        return expressionDeParser;
    }

    /**
     * 处理表达式，当表达式中存在参数，但在params中不存在参数值则用默认占位条件占位
     *
     * @param expression
     */
    public void dealBinaryExpression(Expression expression, Expression parentExpression, TreeNodePosition treeNodePosition) {
        if (expression != null) {
            if (parentExpression != null && !childParentExpressionMap.containsKey(expression)) {
                childParentExpressionMap.put(expression, parentExpression);
            }
            this.expressionAttrPut(expression, parentExpression, treeNodePosition);

            log.info("Class Name:" + expression.getClass().getName());
            if (expression instanceof ComparisonOperator) {
                ComparisonOperator comparisonOperator = (ComparisonOperator) expression;
                comparisonOperator.accept(this);
            } else if (expression instanceof LikeExpression) {
                LikeExpression likeExpression = (LikeExpression) expression;
                likeExpression.accept(this);
            } else if (expression instanceof InExpression) {
                InExpression inExpression = (InExpression) expression;
                inExpression.accept(this);
            } else if (expression instanceof ExistsExpression) {
                ExistsExpression existsExpression = (ExistsExpression) expression;
                existsExpression.accept(this);
            } else if (expression instanceof BinaryExpression) {
                BinaryExpression binaryExpression = (BinaryExpression) expression;
                Expression leftExpression = binaryExpression.getLeftExpression();
                Expression rightExpression = binaryExpression.getRightExpression();

                dealBinaryExpression(rightExpression, binaryExpression, TreeNodePosition.RIGHT);
                dealBinaryExpression(leftExpression, binaryExpression, TreeNodePosition.LEFT);


            } else if (expression instanceof Between) {
                Between between = (Between) expression;
                between.accept(this);
            } else if (expression instanceof Parenthesis) {
                Parenthesis parenthesis = (Parenthesis) expression;
                parenthesis.accept(this);
            } else {
            }
        }

    }

    public void expressionAttrPut(Expression expression, Expression parentExpression, TreeNodePosition treeNodePosition) {
        ExpressionAttr expressionAttr = ExpressionAttr.builder().expression(expression).build();
        switch (treeNodePosition) {
            case ROOT:
                expressionAttr.setRoot(true);
                break;
            case LEFT:
                expressionAttr.setLeft(true);
                break;
            default:
                expressionAttr.setRigth(true);
                break;
        }
        if (parentExpression == null) {
            expressionAttr.setRoot(true);
        }
        expressionAttrMap.put(expression, expressionAttr);
    }


    /**
     * 如果传入的表达式需要被移除，此方法相对难理解  最好笔画一颗树出来容易理解<br/>
     * 因为传入的表达式是在二叉树的有节点，需要移除则需如下步骤，分两种情况<br/>
     * 1、若果表达式不是紧跟在where条件后的 执行如下操作<br/>
     * 1）、找到移除表达式的父节点<br/>
     * 2）、找到移除表达式的父节点的左节点<br/>
     * 4）、移除表达式的父节点的左节点的左节点赋值移除表达式的父节点的左节点<br/>
     * 5）、移除表达式的父节点的左节点的右节点赋值移除表达式的父节点的右节点<br/>
     * 6）、更新{@link childParentExpressionMap}中的节点关系<br/>
     * 2、若果表达式紧跟在where条件后的 执行如下操作<br/>
     * 1）、找到移除表达式的父节点<br/>
     * 2）、找到移除表达式的父节点的右节点<br/>
     * 4）、移除表达式的父节点的右节点赋值移除表达式的父节点的父节点的左节点<br/>
     * 5）、移除表达式的父节点的右节点的右节点赋值移除表达式的父节点的右节点<br/>
     * 6）、更新{@link childParentExpressionMap}中的节点关旭<br/>
     *
     * @param isRemoveCondition true:传入的表达式需要被移除
     * @param expression
     */
    private void retainExpression(boolean isRemoveCondition, Expression expression) {
        if (!isRemoveCondition) {
            return;
        }
        PlainSelect plainSelect = null;
        if (this.selectBody instanceof PlainSelect) {
            plainSelect = (PlainSelect) this.selectBody;
        }

        //System.out.println("plainSelect:"+plainSelect.toString());
        //找到该表达式的父节点 (表达式的1级父表达式)
        Expression eLevel1Parent = childParentExpressionMap.get(expression);

        if (eLevel1Parent != null && eLevel1Parent instanceof BinaryExpression) {
            BinaryExpression eLevel1Parent2Be = (BinaryExpression) eLevel1Parent;
            //找到该表达式的父节点的左节点
            Expression eLevel1ParentsL = eLevel1Parent2Be.getLeftExpression();

            //此情况正常是 ： expression 为SQL中where后的第一个条件  即树节点的最左叶子节点和最左叶子节点的父节点
            ExpressionAttr expressionAttr = expressionAttrMap.get(expression);
            if ((expressionAttr.isLeft()) && (expression instanceof Between
                    || expression instanceof InExpression
                    || expression instanceof BinaryExpression)) {
                Expression expressionChileLeft = null;
                if (expression instanceof Between) {
                    expressionChileLeft = ((Between) expression).getLeftExpression();
                } else if (expression instanceof InExpression) {
                    expressionChileLeft = ((InExpression) expression).getLeftExpression();
                }
                if (expression instanceof BinaryExpression) {
                    expressionChileLeft = ((BinaryExpression) expression).getLeftExpression();
                }
                if (expressionChileLeft instanceof Column || expressionChileLeft instanceof Function) {
                    Expression rightExpression2 = eLevel1Parent2Be.getRightExpression();
                    Expression expression2LevelParent = childParentExpressionMap.get(eLevel1Parent2Be);

                    if (expression2LevelParent == null) {//此情况一般为where后仅两个条件，其中第一个是要删除的条件，第二要需要保留的条件
                        if (plainSelect != null && plainSelect.getWhere() != null) {
                            if (removeExpressionSet.contains(rightExpression2)) {
                                plainSelect.setWhere(null);
                            } else {
                                plainSelect.setWhere(rightExpression2);
                            }
                            childParentExpressionMap.clear();
                            removeExpressionSet.add(expression);
                            return;//处理完返回  不能再继续处理
                        }
                    } else {
                        if (expression2LevelParent instanceof BinaryExpression) {
                            BinaryExpression expression2LevelParentTO = (BinaryExpression) expression2LevelParent;
                            expression2LevelParentTO.setLeftExpression(rightExpression2);
                            removeExpressionSet.add(expression);
                            childParentExpressionMap.put(rightExpression2, expression2LevelParentTO);
                            if (expressionAttrMap.get(rightExpression2) != null && expressionAttrMap.get(rightExpression2).isRigth()) {
                                expressionAttrMap.get(rightExpression2).setLeft(true);
                            }
                            return;//处理完返回  不能再继续处理
                        } else if (expression2LevelParent instanceof Parenthesis) {
                            if (eLevel1Parent2Be.getLeftExpression() == expression) { //因为Parenthesis类中 是先遍历左节点后遍历右节点 所以动态判断
                                ((Parenthesis) expression2LevelParent).setExpression(eLevel1Parent2Be.getRightExpression());
                            } else {
                                ((Parenthesis) expression2LevelParent).setExpression(eLevel1Parent2Be.getLeftExpression());
                            }
                        }
                    }
                }
            }
            //传入表达式非where后的第一个条件
            if (eLevel1ParentsL != null) {
                Expression expressionLevel2Parent = childParentExpressionMap.get(eLevel1Parent2Be);//参数表达式的父节点的父节点

                if (eLevel1ParentsL instanceof BinaryExpression && !(eLevel1ParentsL instanceof ComparisonOperator)) { //左边是二分表达式；  a=1 是二分表达式、 a between 1 and 10 不是二分表达式
                    BinaryExpression leftBinaryExpression = (BinaryExpression) eLevel1ParentsL;
                    //替换表达式的父节点的左节点的左节点
                    Expression leftExpression3 = leftBinaryExpression.getLeftExpression();
                    //替换表达式的父节点的左节点的右节点
                    Expression rightExpression3 = leftBinaryExpression.getRightExpression();

                    //替换表达式的父节点的左节点的左节点已经是列名了 一般在紧跟在sql where后 ,此时参数expression为一般在sql where后第二个条件
                    if (leftExpression3 instanceof Column || leftExpression3 instanceof Function) {
                        if (expressionLevel2Parent != null && expressionLevel2Parent instanceof BinaryExpression) {
                            BinaryExpression binaryExpression2 = (BinaryExpression) expressionLevel2Parent;
                            binaryExpression2.setLeftExpression(leftBinaryExpression);
                            //binaryExpression2.setRightExpression(parentBinaryExpression.getRightExpression());
                            //此时的leftBinaryExpression一般紧跟在 SQL中的where后 ，
                            childParentExpressionMap.put(leftBinaryExpression, binaryExpression2);
                            removeExpressionSet.add(expression);
                        } else {//where 后只有两个条件的情况
                            if (plainSelect != null && plainSelect.getWhere() != null) {
                                plainSelect.setWhere(leftBinaryExpression);
                                childParentExpressionMap.clear();
                                removeExpressionSet.add(expression);
                            }
                        }
                    } else {
                        //移除表达式的父节点的左节点的左节点赋值移除表达式的父节点的左节点
                        eLevel1Parent2Be.setLeftExpression(leftExpression3);
                        //移除表达式的父节点的左节点的右节点赋值移除表达式的父节点的右节点
                        eLevel1Parent2Be.setRightExpression(rightExpression3);
                        //更新子节点和父节点的关系
                        childParentExpressionMap.put(leftExpression3, eLevel1Parent2Be);
                        childParentExpressionMap.put(rightExpression3, eLevel1Parent2Be);
                        removeExpressionSet.add(expression);
                    }
                } else {//有左表达式且不是二分表达式一般为单个条件 如 in、 between
                    if (expressionLevel2Parent == null) {
                        plainSelect.setWhere(eLevel1ParentsL);
                        childParentExpressionMap.clear();
                        removeExpressionSet.add(expression);
                    } else {//传入方法参数表达式存在父节点的父节点  需将父节点的左节点赋值给父节点的父节点的左节点
                        if (expressionLevel2Parent != null && expressionLevel2Parent instanceof BinaryExpression) {
                            BinaryExpression binaryExpression2 = (BinaryExpression) expressionLevel2Parent;
                            binaryExpression2.setLeftExpression(eLevel1Parent2Be.getLeftExpression());
                            //eLevel1Parent2Be.setRightExpression(binaryExpression2.getRightExpression());
                            childParentExpressionMap.put(eLevel1Parent2Be.getLeftExpression(), binaryExpression2);


                        }
                    }
                }
            }
        } else { //不存在父级表达式  标识where后只有一个条件了
            log.info("未替换表达式：{}", expression);
            if (plainSelect != null && plainSelect.getWhere() != null) {
                plainSelect.setWhere(null);
                childParentExpressionMap.clear();
                removeExpressionSet.add(expression);
            }
        }



       /* BinaryExpression parentBinaryExpression = (BinaryExpression) parentExpression;
        Expression leftExpression2 = parentBinaryExpression.getLeftExpression();
        Expression rightExpression2 = parentBinaryExpression.getRightExpression();
        if (newExpression == null) {
            newExpression = expression;
        }
        Expression newExpressionParentExpression = childParentExpressionMap.get(newExpression);
        //parentBinaryExpression.setLeftExpression(expression);
        BinaryExpression newExpressionParentBinaryExpression = (BinaryExpression) newExpressionParentExpression;
        if (newExpressionParentBinaryExpression == null) {
            System.out.println("11111");
        }
        ///newExpressionParentBinaryExpression.setLeftExpression(expression);

        newExpression = newExpressionParentBinaryExpression;

        newExpressionParentBinaryExpression.setRightExpression(newExpression);*/
    }


    public void dealSelectItem(List<SelectItem> selectItems) {
        if (selectItems != null) {
            for (SelectItem selectItem : selectItems) {
                if (selectItem instanceof SelectExpressionItem) {
                    SelectExpressionItem expressionItem = (SelectExpressionItem) selectItem;
                    Expression expression = expressionItem.getExpression();
                    List<Param> paramList = ParamUtil.parseAllParam(expression.toString());
                    System.out.println(paramList);
                    if (paramList != null && paramList.size() > 0) {
                        for (Param param : paramList) {
                            String name = param.getName();
                            Object value = this.params.get(name);
                            param.setValue(value);
                            SqlCondition sqlCondition = SqlCondition.builder().name(name).expression(expression).param(param).build();
                            paramExpression.put(name, sqlCondition);
                            if (value == null) {
                                expressionItem.setExpression(new NullValue());
                            }
                        }
                    }
                }
            }
        }
    }

    public void dealSubSelect(SubSelect subSelect) {
        if (subSelect != null) {
            SelectBody selectBody = subSelect.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                Expression expression = plainSelect.getWhere();

                //值替换走该段代码
                //dealBinaryExpression(expression, null,null);

                this.newExpressionDeParserAndDealPlainSelect(plainSelect);
            } else if (selectBody instanceof SetOperationList) {
                SetOperationList operationList = (SetOperationList) selectBody;
                this.dealSetOperationList(operationList);
            }
        }
    }

    /**
     * UNION 联合的多条SQL语句
     *
     * @param operationList
     */
    public void dealSetOperationList(SetOperationList operationList) {
        List<SelectBody> selects = operationList.getSelects();
        if (selects != null) {
            for (SelectBody select : selects) {
                if (select instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) select;
                    this.newExpressionDeParserAndDealPlainSelect(plainSelect);
                }
            }
        }
    }

    /**
     * 处理join 查询
     * @param joins
     */
    public void dealJoins(List<Join> joins) {
        if (joins != null) {
            for (Join join : joins) {
                FromItem fromItem = join.getRightItem();//可能是子查询  也可能是表
                this.dealFromItem(fromItem);
                Collection<Expression> onExpressions = join.getOnExpressions();//处理on表达式
                if (onExpressions != null) {
                    for (Expression expression : onExpressions) {
                        //生成 只有where的 sql解析条件
                        PlainSelect plainSelect=new PlainSelect();
                        plainSelect.setWhere(expression);
                        //处理on条件
                        this.newExpressionDeParserAndDealPlainSelect(plainSelect);
                    }

                }
            }
        }
    }

    public void dealFromItem(FromItem fromItem) {
        if (fromItem != null) {
            if (fromItem instanceof SubSelect) {
                SubSelect subSelect = (SubSelect) fromItem;
                dealSubSelect(subSelect);
            }
        }
    }

    @Override
    public void visit(Addition addition) {
        super.visit(addition);
    }

    @Override
    public void visit(AndExpression andExpression) {
        super.visit(andExpression);
    }

    @Override
    public void visit(Between between) {
        super.visit(between);

        Expression leftExpression = between.getLeftExpression();
        Expression betweenExpressionStart = between.getBetweenExpressionStart();
        Expression betweenExpressionEnd = between.getBetweenExpressionEnd();
        String betweenStr = between.toString();
        boolean isRemoveCondition = false;//标识是否包含有 ${}  #{} 形式的参数
        List<Param> paramList = ParamUtil.parseAllParam(betweenStr);
        if (paramList != null) {
            if (params == null || params.size() == 0) {
                isRemoveCondition = true;
            }
            for (Param param : paramList) {
                String name = param.getName();
                Object value = params.get(name);
                param.setValue(value);
                SqlCondition sqlCondition = SqlCondition.builder().name(name).expression(between).param(param).build();
                paramExpression.put(name, sqlCondition);
                if (value == null) {
                    isRemoveCondition = true;
                }
            }
//            if (isRemoveCondition) {
//                between.setLeftExpression(Constant.SQL_PLACEHOLDER_COLUMN);
//                between.setBetweenExpressionStart(Constant.SQL_PLACEHOLDER_VALUE);
//                between.setBetweenExpressionEnd(Constant.SQL_PLACEHOLDER_VALUE);
//            }

        }

        this.retainExpression(isRemoveCondition, between);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        super.visit(equalsTo);
        this.visitRightExpression(equalsTo);
    }

    @Override
    public void visit(Division division) {
        super.visit(division);
    }

    @Override
    public void visit(IntegerDivision division) {
        super.visit(division);
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        super.visit(doubleValue);
    }

    @Override
    public void visit(HexValue hexValue) {
        super.visit(hexValue);
    }

    @Override
    public void visit(NotExpression notExpr) {
        super.visit(notExpr);
    }

    @Override
    public void visit(BitwiseRightShift expr) {
        super.visit(expr);
    }

    @Override
    public void visit(BitwiseLeftShift expr) {
        super.visit(expr);
    }

    @Override
    public void visitOldOracleJoinBinaryExpression(OldOracleJoinBinaryExpression expression, String operator) {
        super.visitOldOracleJoinBinaryExpression(expression, operator);
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        super.visit(greaterThan);
        this.visitRightExpression(greaterThan);
    }

    /**
     * 如果表达式中存在参数，且不存在参数值的情况下将  左表达式、右表达式都用占位符替换掉
     *
     * @param expression
     */
    private void visitRightExpression(Expression expression) {
        if (expression != null   ) {
            boolean isRemoveCondition = false;
            if( expression instanceof  ComparisonOperator){
                isRemoveCondition = this.parseAllParam(expression);
            }else if(expression instanceof BinaryExpression){
                BinaryExpression binaryExpression = (BinaryExpression) expression;
                Expression leftExpression = binaryExpression.getLeftExpression();
                Expression rightExpression = binaryExpression.getRightExpression();//先处理右节点
                //先设置父子关系  此处代码不能删
                setChildParentRelation(binaryExpression);
                if (rightExpression != null) {
                    //String rightExpressionValue = rightExpression.toString();
                    //isRemoveCondition = this.parseAllParam(rightExpression);
                    isRemoveCondition = this.parseAllParam(expression);
                    if (isRemoveCondition) {
                        // binaryExpression.setLeftExpression(Constant.SQL_PLACEHOLDER_COLUMN);
                        //  binaryExpression.setRightExpression(Constant.SQL_PLACEHOLDER_VALUE);
                    }
                }
            }

            this.retainExpression(isRemoveCondition, expression);
        }
    }

    /**
     * 解析表达式在所有的参数并放入paramExpression中，并返回是否含有没有参数值的参数
     *
     * @param expression
     * @return
     */
    public boolean parseAllParam(Expression expression) {
        boolean isRemoveCondition = false;
        if (expression != null) {
            String str = expression.toString();
            List<Param> paramList = ParamUtil.parseAllParam(str);
            if (paramList != null) {
                if (this.params == null || this.params.size() == 0) {
                    isRemoveCondition = true;
                }
                for (Param param : paramList) {
                    String name = param.getName();
                    Object value = this.params.get(name);
                    param.setValue(value);
                    SqlCondition sqlCondition = SqlCondition.builder().name(name).expression(expression).param(param).build();
                    this.paramExpression.put(name, sqlCondition);
                    if (value == null) {
                        isRemoveCondition = true;
                    }
                }
            }
        }
        return isRemoveCondition;
    }

    /**
     * 设置子父表达式关系
     *
     * @param binaryExpression
     */
    public void setChildParentRelation(BinaryExpression binaryExpression) {
        if (binaryExpression != null) {
            Expression leftExpression = binaryExpression.getLeftExpression();
            Expression rightExpression = binaryExpression.getRightExpression();
            if (!childParentExpressionMap.containsKey(leftExpression)) {
                childParentExpressionMap.put(leftExpression, binaryExpression);
                this.expressionAttrPut(leftExpression, binaryExpression, TreeNodePosition.LEFT);
            }
            if (!childParentExpressionMap.containsKey(rightExpression)) {
                childParentExpressionMap.put(rightExpression, binaryExpression);
                this.expressionAttrPut(rightExpression, binaryExpression, TreeNodePosition.RIGHT);
            }
        }

    }


    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        super.visit(greaterThanEquals);
        this.visitRightExpression(greaterThanEquals);
    }

    @Override
    public void visit(InExpression inExpression) {
        super.visit(inExpression);
        boolean isRemoveCondition = false;
        if (inExpression.getRightExpression() != null) {
            Expression rightExpression = inExpression.getRightExpression();
            if (rightExpression instanceof SubSelect) {
                SubSelect subSelect = (SubSelect) rightExpression;
                SelectBody selectBody = subSelect.getSelectBody();

                this.dealSubSelect(subSelect);

                /*if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    Expression expression = plainSelect.getWhere();
                    MyExpressionDeParser expressionDeParser = new MyExpressionDeParser(this.params);
                    expressionDeParser.childParentExpressionMap = this.childParentExpressionMap;
                    expressionDeParser.paramExpression=this.paramExpression;
                    if (expression != null) {
                        //值替换走该段代码
                        //dealBinaryExpression(expression, null);

                        //改变二叉树节点
                        expressionDeParser.dealSelectItem(plainSelect.getSelectItems());
                        expressionDeParser.dealBinaryExpression(expression, null, TreeNodePosition.ROOT);
                        // plainSelect.setWhere(newExpression);
                    }
                }*/
            }
        } else if (inExpression.getRightItemsList() != null) {
            String valueStr = inExpression.getRightItemsList().toString();
            List<Param> paramList = ParamUtil.parseAllParam(valueStr);

            if (paramList != null) {
                if (params == null || params.size() == 0) {
                    isRemoveCondition = true;
                }
                for (Param param : paramList) {
                    String name = param.getName();
                    SqlCondition sqlCondition = SqlCondition.builder().name(name).expression(inExpression).build();
                    paramExpression.put(name, sqlCondition);
                    if (!params.containsKey(name)) {
                        isRemoveCondition = true;
                        break;
                    }
                }
//                if (isRemoveCondition) {
//                    inExpression.setLeftExpression(Constant.SQL_PLACEHOLDER_COLUMN);
//                    ExpressionList expressionList = new ExpressionList();
//                    List<Expression> expressions = new ArrayList<>();
//                    expressions.add(Constant.SQL_PLACEHOLDER_VALUE);
//                    expressionList.setExpressions(expressions);
//                    inExpression.setRightItemsList(expressionList);
//                }
            }
        }
        this.retainExpression(isRemoveCondition, inExpression);

    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {
        super.visit(fullTextSearch);
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        super.visit(signedExpression);
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        super.visit(isNullExpression);
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        super.visit(isBooleanExpression);
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
        super.visit(jdbcParameter);
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        super.visit(likeExpression);
        this.visitRightExpression(likeExpression);
    }


    @Override
    public void visit(ExistsExpression existsExpression) {
        super.visit(existsExpression);

        if (existsExpression.getRightExpression() != null) {
            Expression rightExpression = existsExpression.getRightExpression();
            if (rightExpression instanceof SubSelect) {
                SubSelect subSelect = (SubSelect) rightExpression;
                this.dealSubSelect(subSelect);
            }
        }
    }

    @Override
    public void visit(LongValue longValue) {
        super.visit(longValue);
    }

    @Override
    public void visit(MinorThan minorThan) {
        super.visit(minorThan);
        this.visitRightExpression(minorThan);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        super.visit(minorThanEquals);
        this.visitRightExpression(minorThanEquals);
    }

    @Override
    public void visit(Multiplication multiplication) {
        super.visit(multiplication);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        super.visit(notEqualsTo);
    }

    @Override
    public void visit(NullValue nullValue) {
        super.visit(nullValue);
    }

    @Override
    public void visit(OrExpression orExpression) {
        super.visit(orExpression);
        this.visitRightExpression(orExpression);

    }

    @Override
    public void visit(XorExpression xorExpression) {
        super.visit(xorExpression);
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        //类似查询条件 and (col1=1  or col2=2 or col3=3)
        Expression expression = parenthesis.getExpression();
        //this.dealBinaryExpression(expression, parenthesis, TreeNodePosition.RIGHT);
        childParentExpressionMap.put(expression, parenthesis);
        //该方法会调用到 visitBinaryExpression(BinaryExpression binaryExpression, String operator) 先处理左边后处理右边
        super.visit(parenthesis);


    }

    @Override
    public void visit(StringValue stringValue) {
        super.visit(stringValue);
    }

    @Override
    public void visit(Subtraction subtraction) {
        super.visit(subtraction);
    }

    @Override
    protected void visitBinaryExpression(BinaryExpression binaryExpression, String operator) {
        this.setChildParentRelation(binaryExpression);
        super.visitBinaryExpression(binaryExpression, operator);
    }

    @Override
    public void visit(SubSelect subSelect) {
        super.visit(subSelect);
        this.dealSubSelect(subSelect);
    }

    @Override
    public void visit(Column tableColumn) {
        super.visit(tableColumn);
    }

    @Override
    public void visit(Function function) {
        super.visit(function);
    }

    @Override
    public void visit(ExpressionList expressionList) {
        super.visit(expressionList);
    }

    @Override
    public void visit(NamedExpressionList namedExpressionList) {
        super.visit(namedExpressionList);
    }

    @Override
    public void visit(DateValue dateValue) {
        super.visit(dateValue);
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        super.visit(timestampValue);
    }

    @Override
    public void visit(TimeValue timeValue) {
        super.visit(timeValue);
    }

    @Override
    public void visit(CaseExpression caseExpression) {
        super.visit(caseExpression);
    }

    @Override
    public void visit(WhenClause whenClause) {
        super.visit(whenClause);
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        super.visit(anyComparisonExpression);
    }

    @Override
    public void visit(Concat concat) {
        super.visit(concat);
    }

    @Override
    public void visit(Matches matches) {
        super.visit(matches);
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        super.visit(bitwiseAnd);
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        super.visit(bitwiseOr);
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        super.visit(bitwiseXor);
    }

    @Override
    public void visit(CastExpression cast) {
        super.visit(cast);
    }

    @Override
    public void visit(TryCastExpression cast) {
        super.visit(cast);
    }

    @Override
    public void visit(Modulo modulo) {
        super.visit(modulo);
    }

    @Override
    public void visit(AnalyticExpression aexpr) {
        super.visit(aexpr);
    }

    @Override
    public void visit(ExtractExpression eexpr) {
        super.visit(eexpr);
    }

    @Override
    public void visit(MultiExpressionList multiExprList) {
        super.visit(multiExprList);
    }

    @Override
    public void visit(IntervalExpression iexpr) {
        super.visit(iexpr);
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        super.visit(jdbcNamedParameter);
    }

    /**
     * Oracle Oracle connect by 查询
     *
     * @param oexpr
     */
    @Override
    public void visit(OracleHierarchicalExpression oexpr) {
        if (oexpr != null) {
            super.visit(oexpr);
            boolean isRemoveCondition = this.parseAllParam(oexpr);
            if (isRemoveCondition) {
                if (this.selectBody != null && this.selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) this.selectBody;
                    plainSelect.setOracleHierarchical(null);
                }

            }

        }
    }

    @Override
    public void visit(RegExpMatchOperator rexpr) {
        super.visit(rexpr);
    }

    @Override
    public void visit(RegExpMySQLOperator rexpr) {
        super.visit(rexpr);
    }

    @Override
    public void visit(JsonExpression jsonExpr) {
        super.visit(jsonExpr);
    }

    @Override
    public void visit(JsonOperator jsonExpr) {
        super.visit(jsonExpr);
    }

    @Override
    public void visit(UserVariable var) {
        super.visit(var);
    }

    @Override
    public void visit(NumericBind bind) {
        super.visit(bind);
    }

    @Override
    public void visit(KeepExpression aexpr) {
        super.visit(aexpr);
    }

    @Override
    public void visit(MySQLGroupConcat groupConcat) {
        super.visit(groupConcat);
    }

    @Override
    public void visit(ValueListExpression valueList) {
        super.visit(valueList);
    }

    @Override
    public void visit(RowConstructor rowConstructor) {
        super.visit(rowConstructor);
    }

    @Override
    public void visit(RowGetExpression rowGetExpression) {
        super.visit(rowGetExpression);
    }

    @Override
    public void visit(OracleHint hint) {
        super.visit(hint);
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        super.visit(timeKeyExpression);
    }

    @Override
    public void visit(DateTimeLiteralExpression literal) {
        super.visit(literal);
    }

    @Override
    public void visit(NextValExpression nextVal) {
        super.visit(nextVal);
    }

    @Override
    public void visit(CollateExpression col) {
        super.visit(col);
    }

    @Override
    public void visit(SimilarToExpression expr) {
        super.visit(expr);
    }

    @Override
    public void visit(ArrayExpression array) {
        super.visit(array);
    }

    @Override
    public void visit(ArrayConstructor aThis) {
        super.visit(aThis);
    }

    @Override
    public void visit(VariableAssignment var) {
        super.visit(var);
    }

    @Override
    public void visit(XMLSerializeExpr expr) {
        super.visit(expr);
    }

    @Override
    public void visit(TimezoneExpression var) {
        super.visit(var);
    }

    @Override
    public void visit(JsonAggregateFunction expression) {
        super.visit(expression);
    }

    @Override
    public void visit(JsonFunction expression) {
        super.visit(expression);
    }

    @Override
    public void visit(ConnectByRootOperator connectByRootOperator) {
        super.visit(connectByRootOperator);
    }

    @Override
    public void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter) {
        super.visit(oracleNamedFunctionParameter);
    }

    @Override
    public void visit(AllColumns allColumns) {
        super.visit(allColumns);
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        super.visit(allTableColumns);
    }

    @Override
    public void visit(AllValue allValue) {
        super.visit(allValue);
    }

    @Override
    public void visit(IsDistinctExpression isDistinctExpression) {
        super.visit(isDistinctExpression);
    }

    @Override
    public void visit(GeometryDistance geometryDistance) {
        super.visit(geometryDistance);
    }
}
