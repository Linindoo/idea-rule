package cn.olange.model;

@FunctionalInterface
public interface Handler<E> {
	void handle(E var1);
}
