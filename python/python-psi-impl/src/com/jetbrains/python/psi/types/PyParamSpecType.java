package com.jetbrains.python.psi.types;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.python.PyNames;
import com.jetbrains.python.psi.AccessDirection;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyQualifiedNameOwner;
import com.jetbrains.python.psi.resolve.PyResolveContext;
import com.jetbrains.python.psi.resolve.RatedResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Type of typing.ParamSpec using in type checker to unify parameters of generic calls
 */
public final class PyParamSpecType implements PyTypeParameterType {
  @NotNull private final String myName;
  @Nullable private final PyQualifiedNameOwner myDeclarationElement;
  @Nullable private final List<PyCallableParameter> myParameters;
  @Nullable private final PyType myDefaultType;
  @Nullable private final PyQualifiedNameOwner myScopeOwner;

  public PyParamSpecType(@NotNull String name) {
    this(name, null, null,  null, null);
  }

  private PyParamSpecType(@NotNull String name,
                          @Nullable PyQualifiedNameOwner declarationElement,
                          @Nullable List<PyCallableParameter> parameters,
                          @Nullable PyType defaultType,
                          @Nullable PyQualifiedNameOwner scopeOwner) {
    myName = name;
    myDeclarationElement = declarationElement;
    myParameters = parameters;
    myDefaultType = defaultType;
    myScopeOwner = scopeOwner;
  }

  @NotNull
  public PyParamSpecType withParameters(@Nullable List<PyCallableParameter> parameters, @NotNull TypeEvalContext context) {
    return new PyParamSpecType(myName, myDeclarationElement, getNonPsiParameters(parameters, context), myDefaultType, myScopeOwner);
  }

  @NotNull
  public PyParamSpecType withDeclarationElement(@Nullable PyQualifiedNameOwner declarationElement) {
    return new PyParamSpecType(myName, declarationElement, myParameters, myDefaultType, myScopeOwner);
  }

  @NotNull
  public PyParamSpecType withScopeOwner(@Nullable PyQualifiedNameOwner scopeOwner) {
    return new PyParamSpecType(myName, myDeclarationElement, myParameters, myDefaultType, scopeOwner);
  }

  @NotNull
  public PyParamSpecType withDefaultType(@Nullable PyType defaultType) {
    return new PyParamSpecType(myName, myDeclarationElement, myParameters, defaultType, myScopeOwner);
  }

  @Nullable
  private static List<PyCallableParameter> getNonPsiParameters(@Nullable List<PyCallableParameter> parameters,
                                                               @NotNull TypeEvalContext context) {
    if (parameters == null) return null;
    return ContainerUtil.map(parameters, it -> {
      if (it.isPositionalContainer()) return PyCallableParameterImpl.positionalNonPsi(it.getName(), it.getType(context));
      if (it.isKeywordContainer()) return PyCallableParameterImpl.keywordNonPsi(it.getName(), it.getType(context));
      return PyCallableParameterImpl.nonPsi(it.getName(), it.getType(context));
    });
  }

  @Nullable
  public List<PyCallableParameter> getParameters() {
    return myParameters;
  }

  @Override
  public @Nullable PyQualifiedNameOwner getDeclarationElement() {
    return myDeclarationElement;
  }

  @Nullable
  @Override
  public List<? extends RatedResolveResult> resolveMember(@NotNull String name,
                                                          @Nullable PyExpression location,
                                                          @NotNull AccessDirection direction,
                                                          @NotNull PyResolveContext resolveContext) {
    return null;
  }

  @Override
  public Object[] getCompletionVariants(String completionPrefix, PsiElement location, ProcessingContext context) {
    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }

  @NotNull
  @Override
  public String getName() {
    if (myParameters == null) {
      return String.format("ParamSpec(\"%s\")", myName);
    }
    else {
      final TypeEvalContext context = TypeEvalContext.codeInsightFallback(null);
      return String.format("[%s]",
                           StringUtil.join(myParameters, param -> {
                                             if (param != null) {
                                               final PyType type = param.getType(context);
                                               return type != null ? type.getName() : PyNames.UNKNOWN_TYPE;
                                             }
                                             return PyNames.UNKNOWN_TYPE;
                                           },
                                           ", "));
    }
  }

  @Override
  public @Nullable PyQualifiedNameOwner getScopeOwner() {
    return myScopeOwner;
  }

  @Override
  @Nullable
  public PyType getDefaultType() {
    return myDefaultType;
  }

  @NotNull
  public String getVariableName() {
    return myName;
  }

  @Override
  public boolean isBuiltin() {
    return false;
  }

  @Override
  public void assertValid(String message) {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final PyParamSpecType type = (PyParamSpecType)o;
    return myName.equals(type.myName) && Objects.equals(myScopeOwner, type.myScopeOwner) && Objects.equals(myParameters, type.myParameters);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
