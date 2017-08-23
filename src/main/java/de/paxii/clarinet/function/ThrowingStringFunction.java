package de.paxii.clarinet.function;

@FunctionalInterface
public interface ThrowingStringFunction<R> {

  /**
   * Applies this function to the given arguments.
   *
   * @param value the first function argument
   * @return the function result
   */
  R apply(String value) throws Exception;
}
