import React, { Component } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

/**
 * ErrorBoundary catches unhandled rendering errors and displays a recovery card
 * instead of letting the application display a blank white page.
 */
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error("ErrorBoundary caught an unhandled runtime exception:", error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen w-full flex-col items-center justify-center bg-gray-50 p-6 text-center">
          <div className="max-w-md w-full bg-white rounded-2xl border border-red-150 p-8 shadow-md space-y-6">
            <div className="rounded-full bg-red-100 p-4 text-red-650 mx-auto w-fit">
              <AlertTriangle className="h-12 w-12" />
            </div>
            
            <div className="space-y-2">
              <h1 className="text-2xl font-black text-gray-900">Something went wrong.</h1>
              <p className="text-base text-gray-550 font-semibold leading-relaxed">
                An unexpected application error occurred. Please try refreshing the page or checking your connection.
              </p>
              <div className="text-xs text-red-500 font-bold bg-red-50 p-2.5 rounded-lg border border-red-100 max-h-32 overflow-y-auto text-left font-mono">
                {this.state.error?.toString()}
              </div>
            </div>

            <button
              onClick={this.handleReset}
              style={{ minHeight: '48px' }}
              className="w-full flex items-center justify-center space-x-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-base font-bold shadow-sm transition-all cursor-pointer"
            >
              <RefreshCw className="h-4.5 w-4.5" />
              <span>Refresh Page</span>
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
