package cn.olange.model;

public class AsyncResult<T> {
	private boolean success;
	private T result;

	public AsyncResult(boolean success, T result) {
		this.success = success;
		this.result = result;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}
}
