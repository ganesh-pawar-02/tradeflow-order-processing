export const getApiErrorMessage = (error) => {
  if (error.response?.data) {
    const { message, errorCode } = error.response.data;

    if (message) {
      return errorCode ? `${message} (${errorCode})` : message;
    }
  }

  if (error.code === "ECONNABORTED") {
    return "The request timed out. Please check the order or product before retrying.";
  }

  if (!error.response) {
    return "Unable to connect to the server. Please check your connection.";
  }

  return "Something went wrong. Please try again.";
};