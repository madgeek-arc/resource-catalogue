package gr.uoa.di.madgik.resourcecatalogue.dto;

import org.springframework.http.HttpStatus;
import gr.uoa.di.madgik.resourcecatalogue.logging.LogTransactionsFilter;
import gr.uoa.di.madgik.resourcecatalogue.utils.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Error reporting class. It is returned every time an exception is thrown.
 */
public class ServerError {

    /**
     * The status code to return.
     */
    int status;
    /**
     * The id of the erroneous transaction.
     */
    String transactionId;
    /**
     * The timestamp the error occurred.
     */
    Date timestamp;
    /**
     * The requested url that the error occurred.
     */
    String url;
    /**
     * The error message to display.
     */
    String message;

    public ServerError() {
        timestamp = new Date();
    }

    public ServerError(String transactionId, String url, String message) {
        timestamp = new Date();
        this.transactionId = transactionId;
        this.url = url;
        this.message = message;
    }

    public ServerError(HttpStatus status, String transactionId, String url, String message) {
        timestamp = new Date();
        this.status = status.value();
        this.transactionId = transactionId;
        this.url = url;
        this.message = message;
    }

    public ServerError(int status, String transactionId, String url, String message) {
        timestamp = new Date();
        this.status = status;
        this.transactionId = transactionId;
        this.url = url;
        this.message = message;
    }

    public ServerError(HttpStatus status, HttpServletRequest req, Exception exception) {
        timestamp = new Date();
        this.status = status.value();
        this.transactionId = LogTransactionsFilter.getTransactionId();
        this.url = RequestUtils.getUrlWithParams(req);
        this.message = exception.getMessage();
    }

    public ServerError(HttpStatus status, String transactionId, HttpServletRequest req, Exception exception) {
        timestamp = new Date();
        this.status = status.value();
        this.transactionId = transactionId;
        this.url = RequestUtils.getUrlWithParams(req);
        this.message = exception.getMessage();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
