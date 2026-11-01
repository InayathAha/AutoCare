import React from 'react';
import type { IApiError } from '../types';

interface ErrorMessageProps {
  error: IApiError | string | null;
  onDismiss?: () => void;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({ error, onDismiss }) => {
  if (!error) return null;

  const isApiError = typeof error !== 'string';
  const message = isApiError ? error.message : error;
  const code = isApiError ? error.code : undefined;
  const fieldErrors = isApiError ? error.fieldErrors : undefined;

  return (
    <div className="error-banner" role="alert">
      <div className="error-header">
        <span className="error-icon">⚠️</span>
        <div className="error-content">
          <strong>{code ? `[${code}] ` : ''}{message}</strong>
          {fieldErrors && fieldErrors.length > 0 && (
            <ul className="field-errors-list">
              {fieldErrors.map((fe, idx) => (
                <li key={idx}>
                  <code>{fe.field}</code>: {fe.message}
                </li>
              ))}
            </ul>
          )}
        </div>
        {onDismiss && (
          <button type="button" className="error-dismiss" onClick={onDismiss} aria-label="Dismiss error">
            &times;
          </button>
        )}
      </div>
    </div>
  );
};
