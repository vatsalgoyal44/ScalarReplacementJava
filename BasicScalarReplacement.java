
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import javassist.compiler.ast.Expr;
import javassist.expr.Cast;

import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import java.util.*;

import org.checkerframework.checker.units.qual.s;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class BasicScalarReplacement {
    public static Map<String, ClassOrInterfaceDeclaration> classMap = new HashMap<String, ClassOrInterfaceDeclaration>();
    public static Map<String, String> replacementMap = new HashMap<String, String>();
    public static Map<String, Integer> escapeStatusMethodMap = new HashMap<String, Integer>();
    public static Map<String, List<String>> objectReferencesMethodMap = new HashMap<String, List<String>>();
    public static List<String> staticFields = new ArrayList<String>();
    public static EqualitySets equalitySets = new EqualitySets();

    
    public static void main(String[] args) {
        String FILE_PATH =
                "src/main/java/com/yourorganization/maven_sample/" + args[0] + ".java";
        try {
            // Parse the Java file
            ReflectionTypeSolver typeSolver = new ReflectionTypeSolver();
            StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));
            CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(Paths.get(FILE_PATH)));
            // Create the visitor and visit the AST
            MethodVisitor methodVisitor = new MethodVisitor();
            ClassParameterVisitor classParameterVisitor = new ClassParameterVisitor();
            

            List<String> objectNames = new ArrayList<>();            
            classParameterVisitor.visit(cu, classMap);
            System.out.println(staticFields);
            methodVisitor.visit(cu, objectNames);
            System.out.println(cu);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Custom visitor class to extract method information
    private static class MethodVisitor extends ModifierVisitor<List<String>> {
        @Override
        public MethodDeclaration visit(MethodDeclaration method, List<String> collector) {
            
            // Continue visiting other methods
            super.visit(method, collector);

            extractObjectInitializations(method);
            // FieldAccessVisitor fieldAccessVisitor = new FieldAccessVisitor();

            // fieldAccessVisitor.visit(method, null);

            System.out.println(escapeStatusMethodMap);
            escapeStatusMethodMap.clear();
            replacementMap.clear();
            equalitySets.clear();
            return method;
        }
    }

    
    private static class FieldAccessVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(FieldAccessExpr fieldAccessExpr, Void arg) {
            super.visit(fieldAccessExpr, arg);

            if(replacementMap.containsKey(fieldAccessExpr.toString())){
                NameExpr nameExpr = new NameExpr(replacementMap.get(fieldAccessExpr.toString()));
                fieldAccessExpr.replace(nameExpr);
            }
        }
    }

    private static class ClassParameterVisitor extends VoidVisitorAdapter<Map<String, ClassOrInterfaceDeclaration>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration classDeclaration, Map<String, ClassOrInterfaceDeclaration> collectorMap) {
            super.visit(classDeclaration, collectorMap);

            // Print class name
            // System.out.println("Class name: " + classDeclaration.getNameAsString());

            // // Visit and print field declarations (class parameters)
            // System.out.println("Class parameters:");
            collectorMap.put(classDeclaration.getNameAsString(), classDeclaration);
            // for (com.github.javaparser.ast.body.VariableDeclarator field : classDeclaration.getFields().get(0).getVariables()) {
            //     System.out.println("Parameter: " + field.getNameAsString());
            //     System.out.println("Type: " + field.getType());
            //     if (field.getInitializer().isPresent()) {
            //         System.out.println("Default Value: " + field.getInitializer().get());
            //     } else {
            //         System.out.println("Default Value: N/A");
            //     }
            // }

            // // Visit and print constructor declarations
            // System.out.println("Constructors:");
            // for (ConstructorDeclaration constructor : classDeclaration.getConstructors()) {
            //     System.out.print(constructor.getNameAsString() + "(");
            //     for (com.github.javaparser.ast.body.Parameter parameter : constructor.getParameters()) {
            //         System.out.print(parameter.getType() + " " + parameter.getNameAsString() + ", ");
            //     }
            //     System.out.println(")");
            // }
            classDeclaration.findAll(FieldDeclaration.class).forEach(field -> {
                if(field.getModifiers().contains(Modifier.staticModifier())){
                    for(VariableDeclarator variable : field.getVariables()){
                        staticFields.add(classDeclaration.getNameAsString() + "." + variable.getNameAsString());
                    }
                    // if(staticFields.containsKey(classDeclaration.getNameAsString())){
                    //     for(VariableDeclarator variable : field.getVariables()){
                    //         staticFields.get(classDeclaration.getNameAsString()).add(variable.getNameAsString());
                    //     }
                    // }
                    // else{
                    //     List<String> arrList = new ArrayList<String>();
                    //     for(VariableDeclarator variable : field.getVariables()){
                    //         arrList.add(classDeclaration.getNameAsString() + "." + variable.getNameAsString());
                    //     }
                    //     staticFields.add(classDeclaration.getNameAsString(), arrList);
                    // }
                }
            });
        }
    }
    
    private static void updateEscapeStateHelper(String varName, Integer target, List<String> visited){
        if(visited.contains(varName)){
            return;
        }
        escapeStatusMethodMap.put(varName, target);
        visited.add(varName);
        if (objectReferencesMethodMap.containsKey(varName)){
            for (String varReferenced : objectReferencesMethodMap.get(varName)){
                if(target > escapeStatusMethodMap.get(varReferenced)){
                    updateEscapeStateHelper(varReferenced, target, visited);
                }
            }
        }
    }
    private static void updateEscapeState(String varName, Integer target){
        List<String> visited = new ArrayList<String>();
        updateEscapeStateHelper(varName, target, visited);
    }

    private static void handleNewDeclaration(String name, String type, int target){        
        escapeStatusMethodMap.put(name, target);
        
        if (classMap.containsKey(type)){
            ClassOrInterfaceDeclaration classDeclaration = classMap.get(type);
            objectReferencesMethodMap.putIfAbsent(name, new ArrayList<String>());
            for(ResolvedFieldDeclaration fieldDeclaration : classDeclaration.resolve().getAllFields()){
                String declaringType = fieldDeclaration.declaringType().getClassName();
                Optional<Node> fieldDec = fieldDeclaration.toAst();
                if(fieldDec.isPresent()){
                    FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                    for (VariableDeclarator variableDeclarator : fieldDeclaration2.getVariables()){
                        String typeChild = variableDeclarator.getTypeAsString();
                        String tempname = name;
                        if(!declaringType.equals(type)){
                            tempname = "(("+declaringType+")"+tempname + ")";
                        }
                        String nameChild = tempname + "." + variableDeclarator.getNameAsString();
                        System.out.println(nameChild);;
                        handleNewDeclaration(nameChild, typeChild, target);
                        objectReferencesMethodMap.get(name).add(nameChild);
                    }
                }
        }
            // for (FieldDeclaration fieldDeclaration : classDeclaration.getFields()){            
            //     for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()){
            //         String typeChild = variableDeclarator.getTypeAsString();
            //         String nameChild = name + "." + variableDeclarator.getNameAsString();
            //         handleNewDeclaration(nameChild, typeChild, target);
            //         objectReferencesMethodMap.get(name).add(nameChild);
            //     }
            // }
        }
        
    }

    private static int replace(String varName, String classType, List<Statement> statements, int index1, String oldName){
        int added = 0;
        statements.remove(index1);
        added -= 1;
        for(ResolvedFieldDeclaration fieldDeclaration : classMap.get(classType).resolve().getAllFields()){
            String declaringType = fieldDeclaration.declaringType().getClassName();
            Optional<Node> fieldDec = fieldDeclaration.toAst();
            if(fieldDec.isPresent()){
                FieldDeclaration fieldDeclaration2 = (FieldDeclaration)fieldDec.get();
                for (VariableDeclarator variableDeclarator : fieldDeclaration2.getVariables()){
                    VariableDeclarator variableDeclarator_new = variableDeclarator.clone();
                    String originalName = variableDeclarator.getNameAsString();
                    String newType = variableDeclarator.getTypeAsString();
                    String newName = "";
                    if(classType.equals(declaringType)){
                        newName = varName + "_" + originalName;
                    }
                    else{
                        newName = varName + "_" + declaringType + "_" + originalName;
                    }
                    variableDeclarator_new.setName(newName);
                    VariableDeclarationExpr variableDeclarationExpr_new = new VariableDeclarationExpr(variableDeclarator_new);
                    ExpressionStmt variablExpressionStmt = new ExpressionStmt(variableDeclarationExpr_new);
                    statements.add(index1, variablExpressionStmt);
                    added += 1;
                    replacementMap.put(oldName + "." + originalName, varName + "_" + originalName);
                    if(classMap.containsKey(newType)){
                        System.out.println(oldName + "." + originalName);
                        if(escapeStatusMethodMap.get(oldName + "." + originalName) <= 1){
                            added += replace(variableDeclarator_new.getNameAsString(), newType, statements, index1, oldName + "." + originalName);
                        }
                    }
                }
            }
        }
        return added;
    }
    private static int scalarReplace(List<Statement> statements, int index){
        Statement statement = statements.get(index);
        if(statement instanceof ExpressionStmt){
            Expression expression = ((ExpressionStmt)statement).getExpression();
            if(expression instanceof VariableDeclarationExpr){
                VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr)expression;
                for(int i = 0; i < variableDeclarationExpr.getVariables().size(); i++){
                    VariableDeclarator variableDeclarator = variableDeclarationExpr.getVariable(i);
                    if(variableDeclarator.getInitializer().isPresent() && variableDeclarator.getInitializer().get() instanceof ObjectCreationExpr){
                        ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)variableDeclarator.getInitializer().get();
                        String classType = variableDeclarator.getTypeAsString();
                        String varName = variableDeclarator.getNameAsString();
                        if(escapeStatusMethodMap.get(varName) <= 1){
                            return replace(varName, classType, statements, index, varName);
                        }
                    }
                }
            }
        }
        List<Expression> expressions = statement.findAll(Expression.class);
        for (Expression expression : expressions){
            if(expression instanceof FieldAccessExpr){
                FieldAccessExpr fieldAccessExpr = (FieldAccessExpr)expression;
                if(replacementMap.containsKey(fieldAccessExpr.toString())){
                    NameExpr nameExpr = new NameExpr(replacementMap.get(fieldAccessExpr.toString()));
                    fieldAccessExpr.replace(nameExpr);
                }
            }
            if(expression instanceof CastExpr){
                CastExpr castExpr = (CastExpr) expression;
                Expression castSubExpression = castExpr.getExpression();
        //     if(castSubExpression instanceof ArrayAccessExpr){
        //         castSubExpression = ((ArrayAccessExpr)castSubExpression).getName();
        //     }
            if (escapeStatusMethodMap.containsKey(castSubExpression.toString())){
                System.out.println(castSubExpression);
            }}
        }
        return 0;
    }

    private static void handleExpression(Expression expression){
        if (expression instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr assignExpr = (VariableDeclarationExpr) expression;
            // if(assignExpr.getVariable(0).getInitializer().get() instanceof ObjectCreationExpr){

            //     ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)assignExpr.getVariable(0).getInitializer().get();
            //     // methodBody.getStatements().remove(i); //Removing the object creating statement
            //     // classReplaced.put(assignExpr.getVariable(0).getNameAsString(), objectCreationExpr.getTypeAsString());
                
            //     // for(FieldDeclaration fieldDeclaration : classMap.get(objectCreationExpr.getTypeAsString()).getFields()){                        
            //         // for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()){
            //         //     // VariableDeclarator variableDeclarator_new = variableDeclarator.clone();
            //         //     // String originalName = variableDeclarator.getNameAsString();
            //         //     // variableDeclarator_new.setName(assignExpr.getVariable(0).getNameAsString() + "_" + originalName);
            //         //     // System.out.println(variableDeclarator_new);
            //         //     // VariableDeclarationExpr variableDeclarationExpr_new = new VariableDeclarationExpr(variableDeclarator_new);
            //         //     // ExpressionStmt variablExpressionStmt = new ExpressionStmt(variableDeclarationExpr_new);
            //         //     // expressions.add(i, variablExpressionStmt)
            //         //     
            //         // }
            //     // }
            // escapeStatusMethodMap.put(assignExpr.getVariable(0).getNameAsString(), 0);
            // handleNewDeclaration(assignExpr.getVariable(0).getNameAsString(), objectCreationExpr.getTypeAsString());
            // }
            for (VariableDeclarator variableDeclarator : assignExpr.getVariables()){
                Optional<Expression> initializerExpression = variableDeclarator.getInitializer();
                int target = 0;
                if (initializerExpression.isPresent()){
                    Expression initializer = initializerExpression.get();
                    if(escapeStatusMethodMap.containsKey(initializer.toString())){
                        objectReferencesMethodMap.putIfAbsent(variableDeclarator.getNameAsString(), new ArrayList<String>());
                        objectReferencesMethodMap.putIfAbsent(initializer.toString(), new ArrayList<String>());
                        objectReferencesMethodMap.get(variableDeclarator.getNameAsString()).add(initializer.toString());
                        objectReferencesMethodMap.get(initializer.toString()).add(variableDeclarator.getNameAsString());
                        target = escapeStatusMethodMap.get(initializer.toString());
                        equalitySets.addPair(variableDeclarator.getNameAsString(), initializer.toString());
                    }
                    else if(initializer instanceof ArrayCreationExpr){
                        Optional<ArrayInitializerExpr> optionalArrayInitializer = ((ArrayCreationExpr)initializer).getInitializer();
                        if(!optionalArrayInitializer.isPresent()){
                            NodeList<ArrayCreationLevel> arrayLevels = ((ArrayCreationExpr)initializer).getLevels();
                            for (ArrayCreationLevel level : arrayLevels){
                                Optional<Expression> creationExpression = level.getDimension();
                                if(!creationExpression.isPresent()){
                                    target = 2;
                                }
                            }
                        }
                        equalitySets.addPair(variableDeclarator.getNameAsString(), variableDeclarator.getNameAsString());
                    }
                    else if(initializer instanceof ObjectCreationExpr){
                        equalitySets.addPair(variableDeclarator.getNameAsString(), variableDeclarator.getNameAsString());
                    }
                }
                escapeStatusMethodMap.put(variableDeclarator.getNameAsString(), target);
                handleNewDeclaration(variableDeclarator.getNameAsString(), assignExpr.getCommonType().asString(), target);
            }
        }

        if (expression instanceof AssignExpr) {
            AssignExpr assignExpr = (AssignExpr) expression;
            Expression value = assignExpr.getValue();
            Expression target = assignExpr.getTarget();

            if (escapeStatusMethodMap.containsKey(value.toString()) && escapeStatusMethodMap.containsKey(target.toString())){
                String rhs = value.toString();
                String lhs = target.toString();
                equalitySets.addPair(lhs, rhs);
            }
            if(value instanceof ArrayAccessExpr){
                value = ((ArrayAccessExpr)value).getName();
                if(escapeStatusMethodMap.containsKey(target.toString())){
                    String lhs = target.toString();
                    equalitySets.addPair(lhs, null);
                }
            }

            if(target instanceof ArrayAccessExpr){
                target = ((ArrayAccessExpr)target).getName();
            }

            if (escapeStatusMethodMap.containsKey(value.toString()) && escapeStatusMethodMap.containsKey(target.toString())){
                String rhs = value.toString();
                String lhs = target.toString();
                objectReferencesMethodMap.putIfAbsent(lhs, new ArrayList<String>());
                objectReferencesMethodMap.putIfAbsent(rhs, new ArrayList<String>());
                objectReferencesMethodMap.get(lhs).add(rhs);
                objectReferencesMethodMap.get(rhs).add(lhs);
            }
            else if(escapeStatusMethodMap.containsKey(value.toString()) && staticFields.contains(target.toString())){
                updateEscapeState(value.toString(), 2);
            }
        }

        // else if (expression instanceof CastExpr){
        //     CastExpr castExpr = (CastExpr) expression;
        //     Expression castSubExpression = castExpr.getExpression();
        //     if(castSubExpression instanceof ArrayAccessExpr){
        //         castSubExpression = ((ArrayAccessExpr)castSubExpression).getName();
        //     }
        //     if (escapeStatusMethodMap.containsKey(castSubExpression.toString())){
        //         if(escapeStatusMethodMap.get(castSubExpression.toString()) < 1){
        //             updateEscapeState(castSubExpression.toString(), 1);
        //         }
        //     }
        // }

        else if (expression instanceof MethodCallExpr){
            MethodCallExpr methodCallExpr = (MethodCallExpr) expression;
            for(Expression argument : methodCallExpr.getArguments()){
                if(argument instanceof ArrayAccessExpr){
                    argument = ((ArrayAccessExpr)argument).getName();
                }
                if(escapeStatusMethodMap.containsKey(argument.toString())){
                    if(escapeStatusMethodMap.get(argument.toString()) < 2){
                        updateEscapeState(argument.toString(), 2);
                    }
                }
            }
        }
        else if (expression instanceof BinaryExpr){
            BinaryExpr binaryExpr = (BinaryExpr)expression;
            BinaryExpr.Operator operator = binaryExpr.getOperator();
            if(operator == BinaryExpr.Operator.EQUALS || operator == BinaryExpr.Operator.GREATER || operator == BinaryExpr.Operator.LESS || operator == BinaryExpr.Operator.GREATER_EQUALS || operator == BinaryExpr.Operator.LESS_EQUALS){
                Expression left = binaryExpr.getLeft();
                Expression right = binaryExpr.getRight();

                if(left instanceof ArrayAccessExpr){
                    left = ((ArrayAccessExpr)left).getName();
                }

                if(right instanceof ArrayAccessExpr){
                    right = ((ArrayAccessExpr)right).getName();
                }

                if(left instanceof NullLiteralExpr){
                    if (escapeStatusMethodMap.containsKey(right.toString()) && escapeStatusMethodMap.get(right.toString()) < 1){
                        updateEscapeState(right.toString(), 1);
                    }
                }
                else if(right instanceof NullLiteralExpr){
                    if (escapeStatusMethodMap.containsKey(left.toString()) && escapeStatusMethodMap.get(left.toString()) < 1){
                        updateEscapeState(left.toString(), 1);
                    }
                }
                else {
                    if (escapeStatusMethodMap.containsKey(left.toString()) && escapeStatusMethodMap.containsKey(right.toString())){
                        if (escapeStatusMethodMap.get(left.toString()) < 1){
                            updateEscapeState(left.toString(), 1);
                        }
                        if (escapeStatusMethodMap.get(right.toString()) < 1){
                            updateEscapeState(right.toString(), 1);
                        }
                    }
                }
            }
        }
    }

    private static void handleStatement(Statement statement){
        if(statement instanceof ReturnStmt){
            ReturnStmt returnStmt = (ReturnStmt) statement;
            Optional<Expression> retExpression = returnStmt.getExpression();
            if (retExpression.isPresent()){
                Expression expression = retExpression.get();
                if(expression instanceof ArrayAccessExpr){
                    expression = ((ArrayAccessExpr)expression).getName();
                }
                if(escapeStatusMethodMap.containsKey(expression.toString())){
                    if (escapeStatusMethodMap.get(expression.toString()) < 2){
                        updateEscapeState(expression.toString(), 2);
                    }
                }
            }
        }
        else if(statement instanceof ThrowStmt){
            ThrowStmt throwStmt = (ThrowStmt) statement;
            Expression expression = throwStmt.getExpression();
            if(expression instanceof ArrayAccessExpr){
                expression = ((ArrayAccessExpr)expression).getName();
            }
            if(escapeStatusMethodMap.containsKey(expression.toString())){
                if (escapeStatusMethodMap.get(expression.toString()) < 2){
                    updateEscapeState(expression.toString(), 2);
                }
            }
        }
    }
    private static void extractObjectInitializations(MethodDeclaration method) {
        // Get the method body
        BlockStmt methodBody = method.getBody().orElse(null);

        if (methodBody == null) {
            return;
        }
        NodeList<Statement> statements = methodBody.getStatements();
        
        // Iterate over the statements inside the method body
        
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            List<Expression> expressions = statement.findAll(Expression.class);
            List<Statement> subStatements = statement.findAll(Statement.class);
            // if (statement instanceof ExpressionStmt) {
            //     ExpressionStmt expressionStmt = (ExpressionStmt) statement;
            //     Expression expression = expressionStmt.getExpression();
            //     // Check if the statement is an object creation expression
                
            // }

            if(statement instanceof ReturnStmt){
                handleStatement(statement);
            }
            else{
                for (Expression expression : expressions){
                    handleExpression(expression);
                }
                for (Statement subStatement : subStatements){
                    handleStatement(subStatement);
                }
            }
        }
        System.out.println(escapeStatusMethodMap);
        for(int i = 0; i < statements.size(); i++){
            i += scalarReplace(statements, i);
        }
    }
}

class EqualitySets{
    Map<String, String> parent = new HashMap<String, String>();

    String getParent(String q){
        if(parent.get(q) == q){
            return q;
        }
        else{
            return getParent(parent.get(q));
        }
    }
    int checkEqual(String a, String b){
        if(getParent(b)!=null && getParent(b).equals(getParent(a))){
            return 0;
        }
        else if(getParent(b)!=null && getParent(a)!=null){
            return 1;
        }
        else{
            return -1;
        }
    }
    void addPair(String a, String b){
        parent.put(a,b);
        if(b!=null && !parent.containsKey(b)){
            parent.put(b,b);
        }
    }
    void clear(){
        parent.clear();
    }
}