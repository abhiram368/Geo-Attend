import React, { useState, useEffect, useRef } from "react";

// Dynamically resolve API URL based on frontend host (handles localhost and LAN/mobile testing)
const getApiUrl = (path) => {
  const host = window.location.hostname || "localhost";
  return `http://${host}:8000${path}`;
};

// ==========================================
// CONSISTENT SVG ICONS LIBRARY
// ==========================================
const DashboardIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="9" rx="1" /><rect x="14" y="3" width="7" height="5" rx="1" /><rect x="14" y="12" width="7" height="9" rx="1" /><rect x="3" y="16" width="7" height="5" rx="1" /></svg>
);
const UsersIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></svg>
);
const MapPinIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" /><circle cx="12" cy="10" r="3" /></svg>
);
const KeyIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><circle cx="7.5" cy="15.5" r="5.5" /><path d="m21 2-9.6 9.6" /><path d="m15.5 7.5 3 3L22 7l-3-3Z" /></svg>
);
const SearchIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" /></svg>
);
const EditIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 20h9" /><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" /></svg>
);
const TrashIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18" /><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" /><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" /><line x1="10" y1="11" x2="10" y2="17" /><line x1="14" y1="11" x2="14" y2="17" /></svg>
);
const FlameIcon = ({ className }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z" /></svg>
);
const RefreshIcon = ({ className }) => (
  <svg className={className} xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" /><path d="M16 3h5v5" /><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" /><path d="M8 21H3v-5" /></svg>
);
const CalendarIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="18" x="3" y="4" rx="2" ry="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" /></svg>
);
const AlertIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" /><line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" /></svg>
);
const ChevronLeftIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6" /></svg>
);
const ChevronRightIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="m9 18 6-6-6-6" /></svg>
);
const SunIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="4" /><path d="M12 2v2" /><path d="M12 20v2" /><path d="m4.93 4.93 1.41 1.41" /><path d="m17.66 17.66 1.41 1.41" /><path d="M2 12h2" /><path d="M20 12h2" /><path d="m6.34 17.66-1.41 1.41" /><path d="m19.07 4.93-1.41 1.41" /></svg>
);
const MoonIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" /></svg>
);
const SystemIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><rect width="20" height="14" x="2" y="3" rx="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" /></svg>
);
const FaceIcon = ({ active }) => {
  const color = active ? "var(--success)" : "var(--text-secondary)";
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 8V5a2 2 0 0 1 2-2h3" />
      <path d="M16 3h3a2 2 0 0 1 2 2v3" />
      <path d="M21 16v3a2 2 0 0 1-2 2h-3" />
      <path d="M8 21H5a2 2 0 0 1-2-2v-3" />
      <path d="M9 10v.01" />
      <path d="M15 10v.01" />
      <path d="M10 14a2 2 0 0 0 4 0" />
      <circle cx="12" cy="12" r="6" />
    </svg>
  );
};
const FingerprintIcon = ({ active }) => {
  const color = active ? "var(--success)" : "var(--text-secondary)";
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 10a2 2 0 0 0-2 2v10" />
      <path d="M14 14a2 2 0 0 0 2-2L16 9a4 4 0 0 0-8 0v3" />
      <path d="M8 17a5 5 0 0 0 10 0v-4a8 8 0 0 0-16 0v2" />
      <path d="M12 2a10 10 0 0 0-10 10v1" />
      <path d="M18 20a6 6 0 0 0 2-4V9a8 8 0 0 0-8-8" />
    </svg>
  );
};
const CheckIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><path d="M20 6 9 17l-5-5" /></svg>
);
const CloseIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg>
);
const MailIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="20" height="16" x="2" y="4" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>
);
const PhoneIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" /></svg>
);

const MessageIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
);
const SupportIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10" /><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" /><line x1="12" y1="17" x2="12.01" y2="17" /></svg>
);
const HistoryIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 8v4l3 3" /><circle cx="12" cy="12" r="10" /><path d="M3.05 11a9 9 0 1 1 .5 4m-.5 5v-5h5" /></svg>
);

function Dashboard() {
  const [adminUser, setAdminUser] = useState(() => {
    try {
      const session = JSON.parse(localStorage.getItem("admin_session"));
      if (session && Date.now() - session.timestamp < 24 * 60 * 60 * 1000) {
        return session.user;
      }
    } catch (e) {
      console.error(e);
    }
    localStorage.removeItem("admin_session");
    return null;
  });

  const [activeTab, setActiveTab] = useState("overview");
  const [theme, setTheme] = useState(() => localStorage.getItem("theme") || "auto");
  const [stats, setStats] = useState({
    total_employees: 0,
    present_today: 0,
    rejected_today: 0,
    active_boundaries: 0,
  });
  const [logs, setLogs] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [boundaries, setBoundaries] = useState([]);
  const [biometricRequests, setBiometricRequests] = useState([]);

  // Feedbacks, Support requests, Admin action logs
  const [feedbacks, setFeedbacks] = useState([]);
  const [supportRequests, setSupportRequests] = useState([]);
  const [adminActionLogs, setAdminActionLogs] = useState([]);

  // Toast and Confirmation Modals
  const [toast, setToast] = useState(null);
  const [confirmDialog, setConfirmDialog] = useState({ isOpen: false, title: "", message: "", onConfirm: null });

  // Add Employee Form Modal State
  const [addEmployeeOpen, setAddEmployeeOpen] = useState(false);
  const [addEmployeeForm, setAddEmployeeForm] = useState({ email: "", full_name: "", password: "", role: "employee", phone: "" });

  // Support Reply Dialog state
  const [replyTicket, setReplyTicket] = useState(null);
  const [replyText, setReplyText] = useState("");

  // Login form states
  const [loginForm, setLoginForm] = useState({ email: "", password: "" });
  const [loginLoading, setLoginLoading] = useState(false);
  const [loginError, setLoginError] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  // Pagination states
  const [overviewPage, setOverviewPage] = useState(1);
  const [employeePage, setEmployeePage] = useState(1);
  const [statsPage, setStatsPage] = useState(1);
  const [feedbackPage, setFeedbackPage] = useState(1);
  const [supportPage, setSupportPage] = useState(1);
  const [adminLogPage, setAdminLogPage] = useState(1);
  const itemsPerPage = 10;

  // Filters and search states
  const [searchQuery, setSearchQuery] = useState("");
  const [logFilter, setLogFilter] = useState("all");
  const [overviewSearch, setOverviewSearch] = useState("");
  const [feedbackSearch, setFeedbackSearch] = useState("");
  const [supportSearch, setSupportSearch] = useState("");
  const [supportStatusFilter, setSupportStatusFilter] = useState("all");
  const [adminLogSearch, setAdminLogSearch] = useState("");

  // Directory Filters
  const [dirRoleFilter, setDirRoleFilter] = useState("all");
  const [dirBioFilter, setDirBioFilter] = useState("all");

  // Biometric tab filters
  const [bioStatusFilter, setBioStatusFilter] = useState("pending");
  const [bioTypeFilter, setBioTypeFilter] = useState("all");
  const [bioDateSort, setBioDateSort] = useState("newest");
  const [bioDateFilter, setBioDateFilter] = useState("all"); // all, today, week, month

  // Individual Stats sub-page state
  const [viewingEmployeeId, setViewingEmployeeId] = useState(null);
  const [employeeLogs, setEmployeeLogs] = useState([]);
  const [loadingEmpLogs, setLoadingEmpLogs] = useState(false);

  // Edit employee modal state
  const [editingEmployee, setEditingEmployee] = useState(null);
  const [editForm, setEditForm] = useState({ full_name: "", email: "", phone: "", role: "" });
  const [editSuccessMsg, setEditSuccessMsg] = useState("");
  const [editErrorMsg, setEditErrorMsg] = useState("");

  // Boundary Form State
  const [newBoundary, setNewBoundary] = useState({
    location_name: "",
    center_latitude: "",
    center_longitude: "",
    radius_meters: "",
  });
  const [editingBoundaryId, setEditingBoundaryId] = useState(null);
  const [boundarySubmitMsg, setBoundarySubmitMsg] = useState({ type: "", text: "" });

  // Map elements refs
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const layersGroupRef = useRef(null);

  // Auto-dismiss toast timer
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Helper function to build auth headers
  const getHeaders = () => {
    const session = JSON.parse(localStorage.getItem("admin_session"));
    return {
      "Content-Type": "application/json",
      ...(session?.user?.email ? { "X-Admin-Email": session.user.email } : {})
    };
  };

  // Custom alert/toast helper
  const showToast = (message, type = "success") => {
    setToast({ message, type });
  };

  // Custom confirm dialog helper
  const openConfirm = (title, message, onConfirm) => {
    setConfirmDialog({ isOpen: true, title, message, onConfirm });
  };

  // Theme effect
  useEffect(() => {
    const root = document.documentElement;
    const updateTheme = () => {
      const systemDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
      const isDark = theme === "dark" || (theme === "auto" && systemDark);
      if (isDark) {
        root.classList.add("dark");
      } else {
        root.classList.remove("dark");
      }
    };
    updateTheme();
    localStorage.setItem("theme", theme);

    if (theme === "auto") {
      const media = window.matchMedia("(prefers-color-scheme: dark)");
      media.addEventListener("change", updateTheme);
      return () => media.removeEventListener("change", updateTheme);
    }
  }, [theme]);

  // Fetch telemetry
  const fetchData = async (showRefreshAnimation = false) => {
    if (!adminUser) return;
    if (showRefreshAnimation) setRefreshing(true);
    setError(null);

    try {
      const headers = getHeaders();
      const [statsRes, logsRes, employeesRes, boundariesRes, biometricRes, feedbacksRes, supportRes, actionLogsRes] = await Promise.all([
        fetch(getApiUrl("/dashboard-stats"), { headers }),
        fetch(getApiUrl("/attendance-logs"), { headers }),
        fetch(getApiUrl("/employees"), { headers }),
        fetch(getApiUrl("/campus-boundaries"), { headers }),
        fetch(getApiUrl("/biometric-requests"), { headers }),
        fetch(getApiUrl("/feedbacks"), { headers }),
        fetch(getApiUrl("/support-requests"), { headers }),
        fetch(getApiUrl("/admin/action-logs"), { headers }),
      ]);

      if (!statsRes.ok || !logsRes.ok || !employeesRes.ok || !boundariesRes.ok) {
        throw new Error("One or more network requests failed to load dashboard telemetry.");
      }

      const [statsData, logsData, employeesData, boundariesData, biometricData, feedbacksData, supportData, actionLogsData] = await Promise.all([
        statsRes.json(),
        logsRes.json(),
        employeesRes.json(),
        boundariesRes.json(),
        biometricRes.ok ? biometricRes.json() : [],
        feedbacksRes.ok ? feedbacksRes.json() : [],
        supportRes.ok ? supportRes.json() : [],
        actionLogsRes.ok ? actionLogsRes.json() : [],
      ]);

      setStats(statsData);
      setLogs(logsData);
      setEmployees(employeesData);
      setBoundaries(boundariesData);
      setBiometricRequests(biometricData);
      setFeedbacks(feedbacksData);
      setSupportRequests(supportData);
      setAdminActionLogs(actionLogsData);
    } catch (err) {
      console.error("Dashboard load failure:", err);
      setError("Unable to connect to the backend server. Make sure FastAPI is running on port 8000.");
    } finally {
      setLoading(false);
      if (showRefreshAnimation) {
        setTimeout(() => setRefreshing(false), 500);
      }
    }
  };

  useEffect(() => {
    if (adminUser) {
      setLoading(true);
      fetchData();
      const interval = setInterval(() => fetchData(), 25000);
      return () => clearInterval(interval);
    } else {
      setLoading(false);
    }
  }, [adminUser]);

  // Fetch specific employee logs
  const fetchEmployeeLogs = async (userId) => {
    setLoadingEmpLogs(true);
    setStatsPage(1);
    try {
      const response = await fetch(getApiUrl(`/attendance-logs/user/${userId}`));
      if (response.ok) {
        const data = await response.json();
        setEmployeeLogs(data);
      }
    } catch (err) {
      console.error("Error fetching employee logs:", err);
    } finally {
      setLoadingEmpLogs(false);
    }
  };

  useEffect(() => {
    if (viewingEmployeeId) {
      fetchEmployeeLogs(viewingEmployeeId);
    }
  }, [viewingEmployeeId]);

  // Leaflet map setup
  useEffect(() => {
    if (activeTab === "boundaries" && boundaries.length > 0 && window.L && mapContainerRef.current) {
      // Re-initialize map on active tab change (destroy old if exists)
      if (mapInstanceRef.current) {
        try {
          mapInstanceRef.current.remove();
        } catch (e) {
          console.error(e);
        }
        mapInstanceRef.current = null;
      }

      const map = window.L.map(mapContainerRef.current).setView([11.3216, 75.9336], 15);
      window.L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: '© OpenStreetMap contributors'
      }).addTo(map);
      mapInstanceRef.current = map;
      layersGroupRef.current = window.L.layerGroup().addTo(map);

      // Draw boundaries
      layersGroupRef.current.clearLayers();
      const latlngs = [];
      boundaries.forEach(b => {
        const color = b.is_active ? "#10b981" : "#ef4444";
        const circle = window.L.circle([b.center_latitude, b.center_longitude], {
          color: color,
          fillColor: color,
          fillOpacity: 0.16,
          radius: b.radius_meters
        });

        circle.bindPopup(`
          <div style="font-family: Inter, sans-serif; font-size: 13px; line-height: 1.5;">
            <strong style="display:block; font-size:14px; margin-bottom:4px; color: var(--text-primary);">${b.location_name}</strong>
            <b>Status:</b> ${b.is_active ? "<span style='color:#10b981;font-weight:700;'>Active</span>" : "<span style='color:#ef4444;font-weight:700;'>Inactive</span>"}<br/>
            <b>Radius:</b> ${b.radius_meters} meters<br/>
            <b>Latitude:</b> ${b.center_latitude.toFixed(6)}<br/>
            <b>Longitude:</b> ${b.center_longitude.toFixed(6)}
          </div>
        `);

        layersGroupRef.current.addLayer(circle);
        latlngs.push([b.center_latitude, b.center_longitude]);
      });

      if (latlngs.length > 0 && mapInstanceRef.current) {
        mapInstanceRef.current.fitBounds(latlngs, { padding: [40, 40] });
      }
    }

    return () => {
      if (mapInstanceRef.current) {
        try {
          mapInstanceRef.current.remove();
        } catch (e) {
          console.error(e);
        }
        mapInstanceRef.current = null;
        layersGroupRef.current = null;
      }
    };
  }, [activeTab, boundaries]);

  // Toggle boundary status
  const handleToggleBoundary = async (boundaryId, currentStatus) => {
    try {
      const response = await fetch(getApiUrl(`/campus-boundaries/${boundaryId}`), {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ is_active: !currentStatus }),
      });

      if (!response.ok) throw new Error("Failed to toggle boundary state.");

      setBoundaries(boundaries.map(b => b.id === boundaryId ? { ...b, is_active: !currentStatus } : b));
      const statsRes = await fetch(getApiUrl("/dashboard-stats"), { headers: getHeaders() });
      if (statsRes.ok) {
        setStats(await statsRes.json());
      }
      showToast("Geofence boundary status updated!");
    } catch (err) {
      showToast("Error updating boundary status: " + err.message, "error");
    }
  };

  // Delete boundary
  const handleDeleteBoundary = async (boundaryId) => {
    openConfirm(
      "Delete Geofence",
      "Are you sure you want to permanently delete this campus boundary? Active geofences regulate employee coordinate verifications.",
      async () => {
        try {
          const response = await fetch(getApiUrl(`/campus-boundaries/${boundaryId}`), {
            method: "DELETE",
            headers: getHeaders()
          });

          if (!response.ok) throw new Error("Failed to delete boundary.");

          setBoundaries(boundaries.filter(b => b.id !== boundaryId));
          if (editingBoundaryId === boundaryId) {
            setEditingBoundaryId(null);
            setNewBoundary({ location_name: "", center_latitude: "", center_longitude: "", radius_meters: "" });
          }

          const statsRes = await fetch(getApiUrl("/dashboard-stats"), { headers: getHeaders() });
          if (statsRes.ok) {
            setStats(await statsRes.json());
          }
          showToast("Campus boundary deleted successfully.");
        } catch (err) {
          showToast("Error deleting boundary: " + err.message, "error");
        }
      }
    );
  };

  // Add/Edit boundary form submission
  const handleBoundarySubmit = async (e) => {
    e.preventDefault();
    setBoundarySubmitMsg({ type: "", text: "" });

    const { location_name, center_latitude, center_longitude, radius_meters } = newBoundary;
    if (!location_name || !center_latitude || !center_longitude || !radius_meters) {
      setBoundarySubmitMsg({ type: "error", text: "Please fill out all boundary parameters." });
      return;
    }

    try {
      let response;
      if (editingBoundaryId) {
        response = await fetch(getApiUrl(`/campus-boundaries/${editingBoundaryId}`), {
          method: "PUT",
          headers: getHeaders(),
          body: JSON.stringify({
            location_name,
            center_latitude: parseFloat(center_latitude),
            center_longitude: parseFloat(center_longitude),
            radius_meters: parseFloat(radius_meters)
          }),
        });
      } else {
        response = await fetch(getApiUrl("/campus-boundaries"), {
          method: "POST",
          headers: getHeaders(),
          body: JSON.stringify({
            location_name,
            center_latitude: parseFloat(center_latitude),
            center_longitude: parseFloat(center_longitude),
            radius_meters: parseFloat(radius_meters),
            is_active: true
          }),
        });
      }

      if (!response.ok) throw new Error("Could not save geofence configuration.");

      const saved = await response.json();
      if (editingBoundaryId) {
        setBoundaries(boundaries.map(b => b.id === editingBoundaryId ? saved : b));
        setBoundarySubmitMsg({ type: "success", text: `Successfully updated ${location_name} geofence!` });
        setEditingBoundaryId(null);
      } else {
        setBoundaries([...boundaries, saved]);
        setBoundarySubmitMsg({ type: "success", text: `Successfully registered ${location_name} geofence!` });
      }

      setNewBoundary({ location_name: "", center_latitude: "", center_longitude: "", radius_meters: "" });
      const statsRes = await fetch(getApiUrl("/dashboard-stats"), { headers: getHeaders() });
      if (statsRes.ok) {
        setStats(await statsRes.json());
      }
    } catch (err) {
      setBoundarySubmitMsg({ type: "error", text: err.message });
    }
  };

  const handleEditBoundaryClick = (b) => {
    setEditingBoundaryId(b.id);
    setNewBoundary({
      location_name: b.location_name,
      center_latitude: b.center_latitude.toString(),
      center_longitude: b.center_longitude.toString(),
      radius_meters: b.radius_meters.toString(),
    });
    setBoundarySubmitMsg({ type: "", text: "" });
  };

  const handleCancelBoundaryEdit = () => {
    setEditingBoundaryId(null);
    setNewBoundary({ location_name: "", center_latitude: "", center_longitude: "", radius_meters: "" });
    setBoundarySubmitMsg({ type: "", text: "" });
  };

  // Biometric requests approvals/rejections
  const handleActionBiometricRequest = async (requestId, action) => {
    try {
      const response = await fetch(getApiUrl(`/biometric-requests/${requestId}/${action}`), {
        method: "POST",
        headers: getHeaders()
      });

      if (!response.ok) throw new Error(`Failed to ${action} biometric request.`);

      showToast(`Biometric request successfully ${action}ed!`);

      const res = await fetch(getApiUrl("/biometric-requests"), { headers: getHeaders() });
      if (res.ok) {
        setBiometricRequests(await res.json());
      }
      const empRes = await fetch(getApiUrl("/employees"), { headers: getHeaders() });
      if (empRes.ok) {
        setEmployees(await empRes.json());
      }
    } catch (err) {
      showToast("Error handling request: " + err.message, "error");
    }
  };

  // Edit employee popup submit
  const handleEditEmployeeSubmit = async (e) => {
    e.preventDefault();
    setEditSuccessMsg("");
    setEditErrorMsg("");

    if (!editForm.full_name || !editForm.email) {
      setEditErrorMsg("Name and Email are required fields.");
      return;
    }

    try {
      const response = await fetch(getApiUrl(`/users/${editingEmployee.id}`), {
        method: "PUT",
        headers: getHeaders(),
        body: JSON.stringify({
          full_name: editForm.full_name,
          email: editForm.email,
          phone: editForm.phone,
          role: editForm.role,
        }),
      });

      if (!response.ok) {
        const err = await response.json();
        throw new Error(err.detail || "Failed to update profile.");
      }

      const updated = await response.json();

      setEmployees(employees.map(emp => emp.id === editingEmployee.id ? { ...emp, ...updated } : emp));
      showToast("Employee details updated successfully!");
      setEditingEmployee(null);
    } catch (err) {
      setEditErrorMsg(err.message);
    }
  };

  // Delete employee account
  const handleDeleteEmployee = async (emp) => {
    openConfirm(
      "Delete User Account",
      `Are you sure you want to permanently delete the profile for ${emp.full_name} (${emp.email})? This deletes all dependent logs and biometrics.`,
      async () => {
        try {
          const response = await fetch(getApiUrl(`/users/${emp.id}`), {
            method: "DELETE",
            headers: getHeaders()
          });

          if (!response.ok) throw new Error("Failed to delete user account.");

          setEmployees(employees.filter(e => e.id !== emp.id));
          showToast("User account deleted successfully.");

          const statsRes = await fetch(getApiUrl("/dashboard-stats"), { headers: getHeaders() });
          if (statsRes.ok) {
            setStats(await statsRes.json());
          }
        } catch (err) {
          showToast("Error deleting user: " + err.message, "error");
        }
      }
    );
  };

  // Add employee submit
  const handleAddEmployeeSubmit = async (e) => {
    e.preventDefault();
    if (!addEmployeeForm.full_name || !addEmployeeForm.email || !addEmployeeForm.password) {
      showToast("Name, Email, and Password are required fields.", "error");
      return;
    }

    try {
      const response = await fetch(getApiUrl("/users"), {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify(addEmployeeForm),
      });

      if (!response.ok) {
        const err = await response.json();
        throw new Error(err.detail || "Failed to register user.");
      }

      const created = await response.json();
      setEmployees([...employees, created]);
      showToast("User registered successfully!");
      setAddEmployeeOpen(false);
      setAddEmployeeForm({ email: "", full_name: "", password: "", role: "employee", phone: "" });

      const statsRes = await fetch(getApiUrl("/dashboard-stats"), { headers: getHeaders() });
      if (statsRes.ok) {
        setStats(await statsRes.json());
      }
    } catch (err) {
      showToast(err.message, "error");
    }
  };

  // Reply to support ticket submit
  const handleSendReply = async (e) => {
    e.preventDefault();
    if (!replyText.trim()) {
      showToast("Reply message cannot be empty.", "error");
      return;
    }

    try {
      const response = await fetch(getApiUrl(`/support-requests/${replyTicket.id}/reply`), {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify({ reply: replyText }),
      });

      if (!response.ok) throw new Error("Failed to send reply to backend.");

      showToast("Support ticket response submitted!");
      setReplyTicket(null);
      setReplyText("");

      const supportRes = await fetch(getApiUrl("/support-requests"), { headers: getHeaders() });
      if (supportRes.ok) {
        setSupportRequests(await supportRes.json());
      }
    } catch (err) {
      showToast("Error submitting reply: " + err.message, "error");
    }
  };

  // Admin login submit
  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setLoginError("");
    if (!loginForm.email || !loginForm.password) {
      setLoginError("Please enter your admin email and password.");
      return;
    }

    setLoginLoading(true);
    try {
      const response = await fetch(getApiUrl("/login"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(loginForm),
      });

      if (!response.ok) {
        const err = await response.json();
        throw new Error(err.detail || "Authentication failed. Double check your credentials.");
      }

      const data = await response.json();
      if (data.role !== "admin") {
        throw new Error("Access Denied: Only administrators can access this control portal.");
      }

      // Save session with 1 day validity
      const session = {
        user: data,
        timestamp: Date.now()
      };
      localStorage.setItem("admin_session", JSON.stringify(session));
      setAdminUser(data);
      showToast(`Welcome back, ${data.full_name}!`);
    } catch (err) {
      setLoginError(err.message);
    } finally {
      setLoginLoading(false);
    }
  };

  // Admin logout
  const handleLogout = () => {
    openConfirm(
      "Confirm Log Out",
      "Are you sure you want to end your current session? You will be returned to the Admin Authentication screen.",
      () => {
        localStorage.removeItem("admin_session");
        setAdminUser(null);
        showToast("Logged out successfully.");
      }
    );
  };

  // Filter logs for overview
  const filteredLogs = logs.filter(log => {
    const isVerified = log.status.toLowerCase() === "verified";
    const isCheckedOut = log.check_out_time !== null;

    // Search query check
    const matchesSearch = log.user_name.toLowerCase().includes(overviewSearch.toLowerCase()) ||
      log.user_email.toLowerCase().includes(overviewSearch.toLowerCase()) ||
      log.location_name.toLowerCase().includes(overviewSearch.toLowerCase());

    if (!matchesSearch) return false;

    if (logFilter === "all") return true;
    if (logFilter === "verified") return isVerified;
    if (logFilter === "rejected") return !isVerified;
    if (logFilter === "checkedout") return isCheckedOut;
    return true;
  });

  // Paginated logs for overview
  const totalOverviewPages = Math.ceil(filteredLogs.length / itemsPerPage) || 1;
  const paginatedOverviewLogs = filteredLogs.slice(
    (overviewPage - 1) * itemsPerPage,
    overviewPage * itemsPerPage
  );

  // Filter employees for directory
  const filteredEmployees = employees.filter(emp => {
    const matchesSearch = emp.full_name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      emp.email.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;

    // Role filter
    if (dirRoleFilter !== "all" && emp.role.toLowerCase() !== dirRoleFilter.toLowerCase()) return false;

    // Biometric filter
    if (dirBioFilter === "face" && !emp.face_registered) return false;
    if (dirBioFilter === "fingerprint" && !emp.fingerprint_registered) return false;
    if (dirBioFilter === "both" && (!emp.face_registered || !emp.fingerprint_registered)) return false;
    if (dirBioFilter === "none" && emp.face_registered && emp.fingerprint_registered) return false;

    return true;
  });

  // Paginated employees
  const totalEmployeePages = Math.ceil(filteredEmployees.length / itemsPerPage) || 1;
  const paginatedEmployees = filteredEmployees.slice(
    (employeePage - 1) * itemsPerPage,
    employeePage * itemsPerPage
  );

  // Filter Biometric requests
  const filteredBioRequests = biometricRequests.filter(req => {
    // Status filter
    if (bioStatusFilter !== "all" && req.status.toLowerCase() !== bioStatusFilter.toLowerCase()) return false;

    // Type filter
    if (bioTypeFilter !== "all" && req.request_type.toLowerCase() !== bioTypeFilter.toLowerCase()) return false;

    // Date range filter
    if (bioDateFilter !== "all" && req.created_at) {
      const reqDate = new Date(req.created_at);
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      if (bioDateFilter === "today") {
        if (reqDate < today) return false;
      } else if (bioDateFilter === "week") {
        const weekAgo = new Date(today);
        weekAgo.setDate(today.getDate() - 7);
        if (reqDate < weekAgo) return false;
      } else if (bioDateFilter === "month") {
        const monthAgo = new Date(today);
        monthAgo.setMonth(today.getMonth() - 1);
        if (reqDate < monthAgo) return false;
      }
    }

    return true;
  }).sort((a, b) => {
    const dateA = new Date(a.created_at);
    const dateB = new Date(b.created_at);
    return bioDateSort === "newest" ? dateB - dateA : dateA - dateB;
  });

  // Individual stats helper calculations
  const calculateStreak = (userLogs) => {
    if (!userLogs || userLogs.length === 0) return 0;
    const verifiedDates = userLogs
      .filter(log => log.status.toLowerCase() === "verified")
      .map(log => {
        const d = new Date(log.check_in_time);
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      });

    if (verifiedDates.length === 0) return 0;

    const uniqueDates = [...new Set(verifiedDates)].sort((a, b) => new Date(b) - new Date(a));
    const today = new Date();
    const dateStr = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

    const todayStr = dateStr(today);
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    const yesterdayStr = dateStr(yesterday);

    if (uniqueDates[0] !== todayStr && uniqueDates[0] !== yesterdayStr) {
      return 0;
    }

    let streak = 1;
    for (let i = 0; i < uniqueDates.length - 1; i++) {
      const current = new Date(uniqueDates[i]);
      const next = new Date(uniqueDates[i + 1]);
      const diffDays = Math.ceil(Math.abs(current - next) / (1000 * 60 * 60 * 24));

      if (diffDays === 1) {
        streak++;
      } else if (diffDays > 1) {
        break;
      }
    }
    return streak;
  };

  const calculateMonthlyStats = (userLogs) => {
    if (!userLogs) return { verifiedCount: 0, rate: 0, daysInMonth: 30 };
    const now = new Date();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();

    const currentMonthLogs = userLogs.filter(log => {
      if (log.status.toLowerCase() !== "verified") return false;
      const d = new Date(log.check_in_time);
      return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
    });

    const uniqueDates = new Set(currentMonthLogs.map(log => new Date(log.check_in_time).getDate()));
    const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
    const verifiedCount = uniqueDates.size;
    const rate = Math.round((verifiedCount / daysInMonth) * 100) || 0;

    return { verifiedCount, rate, daysInMonth };
  };

  // Helper date/time formatters
  const formatDate = (isoString) => {
    if (!isoString) return "N/A";
    const dateObj = new Date(isoString);
    return dateObj.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
  };

  const formatTime = (isoString) => {
    if (!isoString) return "N/A";
    const dateObj = new Date(isoString);
    return dateObj.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
  };

  // Calculate quick metrics for verified, rejected, checkout counts
  const presentNowCount = logs.filter(log => log.status.toLowerCase() === "verified" && log.check_out_time === null).length;
  const checkedOutCount = logs.filter(log => log.check_out_time !== null).length;
  const notCheckedInCount = Math.max(0, stats.total_employees - stats.present_today);
  const attendanceRate = stats.total_employees > 0 ? Math.round((stats.present_today / stats.total_employees) * 100) : 0;

  if (loading && !refreshing) {
    return (
      <div style={{ display: "grid", placeItems: "center", minHeight: "100vh", background: "var(--bg-app)", fontFamily: "var(--font-sans)" }}>
        <div style={{ textAlign: "center" }}>
          <div style={{
            width: 54,
            height: 54,
            border: "5px solid var(--border-input)",
            borderTop: "5px solid var(--primary)",
            borderRadius: "50%",
            animation: "spin 1s linear infinite",
            margin: "0 auto 24px"
          }} />
          <h2 style={{ color: "var(--text-primary)", margin: 0, fontSize: 22, fontWeight: 700 }}>Gathering Geofence Coordinates...</h2>
          <p style={{ color: "var(--text-secondary)", marginTop: 10, fontSize: 14 }}>Polling active campus boundary databases.</p>
        </div>
      </div>
    );
  }

  // Find user name for selected employee stats
  const activeEmployee = employees.find(emp => emp.id === viewingEmployeeId);

  // Show login page if no authenticated admin session
  if (!adminUser) {
    return (
      <div
        style={{
          minHeight: "100vh",
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          background: "var(--bg-app)",
          padding: 24,
        }}
      >
        <form
          onSubmit={handleLoginSubmit}
          className="dashboard-card"
          style={{
            width: "100%",
            maxWidth: 420,
            padding: 36,
            display: "flex",
            flexDirection: "column",
            gap: 18,
          }}
        >
          <h1
            style={{
              margin: 0,
              textAlign: "center",
              color: "var(--text-primary)",
            }}
          >
            Admin Login
          </h1>

          <p
            style={{
              textAlign: "center",
              color: "var(--text-secondary)",
              marginTop: -8,
              marginBottom: 12,
            }}
          >
            Geo-Attend Control Portal
          </p>

          <input
            className="custom-input"
            type="email"
            placeholder="Email"
            value={loginForm.email}
            onChange={(e) =>
              setLoginForm({
                ...loginForm,
                email: e.target.value,
              })
            }
          />

          <input
            className="custom-input"
            type="password"
            placeholder="Password"
            value={loginForm.password}
            onChange={(e) =>
              setLoginForm({
                ...loginForm,
                password: e.target.value,
              })
            }
          />

          {loginError && (
            <div
              style={{
                color: "#ef4444",
                fontSize: 14,
              }}
            >
              {loginError}
            </div>
          )}

          <button
            type="submit"
            className="theme-btn"
            disabled={loginLoading}
            style={{
              justifyContent: "center",
              padding: "12px",
              fontWeight: 700,
            }}
          >
            {loginLoading ? "Signing in..." : "Sign In"}
          </button>
        </form>
      </div>
    );
  }

  return (
    <div style={{ minHeight: "100vh", padding: "32px 20px 60px", maxWidth: 1280, margin: "0 auto" }}>
      {/* Connection Error Callout */}
      {error && (
        <div style={{
          background: "var(--danger-light)",
          border: "1px solid var(--border-color)",
          color: "var(--danger-text)",
          padding: "16px 20px",
          borderRadius: 16,
          marginBottom: 24,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          boxShadow: "var(--shadow-md)"
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <AlertIcon />
            <div>
              <strong style={{ display: "block", fontSize: 15 }}>Connection Alert</strong>
              <span style={{ fontSize: 13.5 }}>{error}</span>
            </div>
          </div>
          <button
            onClick={() => fetchData(true)}
            style={{
              background: "var(--danger)",
              color: "white",
              border: "none",
              padding: "8px 16px",
              borderRadius: 8,
              cursor: "pointer",
              fontWeight: 600,
              fontSize: 13,
              boxShadow: "var(--shadow-sm)"
            }}
          >
            Retry Connection
          </button>
        </div>
      )}

      {/* Dashboard Top Header */}
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 24, marginBottom: 36, flexWrap: "wrap" }}>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
            <span style={{ fontSize: 12, textTransform: "uppercase", letterSpacing: 1.4, color: "var(--primary)", fontWeight: 700 }}>
              Secure Geofencing
            </span>
            <span style={{ background: "var(--primary-light)", color: "var(--primary)", fontSize: 10, padding: "2px 8px", borderRadius: 999, fontWeight: 700 }}>
              ADMIN PORTAL
            </span>
          </div>
          <h1 className="animated-gradient-title" style={{ margin: "6px 0 0", fontSize: 36 }}>
            Geo-Attend Core Control
          </h1>
          <p style={{ margin: "8px 0 0", color: "var(--text-secondary)", fontSize: 15, maxWidth: 640 }}>
            Live verification maps, boundary coordinate configurations, and complete employee records.
          </p>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          {/* Theme switcher controls */}
          <div style={{ display: "inline-flex", background: "var(--bg-card)", border: "1px solid var(--border-color)", padding: 4, borderRadius: 12, boxShadow: "var(--shadow-sm)" }}>
            {[
              { id: "light", icon: <SunIcon />, label: "Light" },
              { id: "dark", icon: <MoonIcon />, label: "Dark" },
              { id: "auto", icon: <SystemIcon />, label: "Auto" }
            ].map(opt => (
              <button
                key={opt.id}
                onClick={() => setTheme(opt.id)}
                title={`${opt.label} Mode`}
                style={{
                  padding: "8px",
                  borderRadius: 8,
                  border: "none",
                  cursor: "pointer",
                  background: theme === opt.id ? "var(--primary-light)" : "transparent",
                  color: theme === opt.id ? "var(--primary)" : "var(--text-secondary)",
                  display: "grid",
                  placeItems: "center",
                  transition: "all var(--transition-speed) ease"
                }}
              >
                {opt.icon}
              </button>
            ))}
          </div>

          {/* Refresh Button */}
          <button
            onClick={() => fetchData(true)}
            disabled={refreshing}
            title="Refresh telemetry"
            className="theme-btn"
            style={{ width: 42, height: 42, display: "grid", placeItems: "center" }}
          >
            <RefreshIcon className={refreshing ? "spin-slow" : ""} />
          </button>

          {/* Date widget card */}
          <div className="theme-btn" style={{ height: 42, padding: "0 16px" }}>
            <CalendarIcon />
            <span style={{ fontSize: 13.5 }}>
              {new Date().toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" })}
            </span>
          </div>

          {/* Log Out Button */}
          <button
            onClick={handleLogout}
            className="theme-btn"
            style={{ height: 42, padding: "0 16px", background: "var(--danger-light)", color: "var(--danger)", border: "1px solid rgba(239, 68, 68, 0.2)", cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}
          >
            <CloseIcon />
            <span style={{ fontWeight: 600, fontSize: 13 }}>Log Out</span>
          </button>
        </div>
      </header>

      {/* Main Tab Controller navigation */}
      <nav style={{
        display: "inline-flex",
        background: "var(--bg-card)",
        border: "1px solid var(--border-color)",
        padding: 5,
        borderRadius: 16,
        marginBottom: 32,
        boxShadow: "var(--shadow-sm)",
        flexWrap: "wrap",
        gap: 4
      }}>
        {[
          { id: "overview", label: "Overview Dashboard", icon: <DashboardIcon /> },
          { id: "employees", label: "Employee Directory", icon: <UsersIcon /> },
          { id: "boundaries", label: "Campus Boundaries", icon: <MapPinIcon /> },
          { id: "biometric_requests", label: "Biometric Requests", icon: <KeyIcon /> },
          { id: "feedbacks", label: "Feedback Logs", icon: <MessageIcon /> },
          { id: "support", label: "Support Tickets", icon: <SupportIcon /> },
          { id: "admin_logs", label: "Admin Action Logs", icon: <HistoryIcon /> }
        ].map((tab) => {
          const isSelected = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => {
                setActiveTab(tab.id);
                setViewingEmployeeId(null); // Reset detail page
                setBoundarySubmitMsg({ type: "", text: "" });
              }}
              className={`nav-tab-btn ${isSelected ? "active" : ""}`}
            >
              {tab.icon}
              <span>{tab.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Primary Telemetry Containers */}
      <main>
        {/* ========================================== */}
        {/* TAB 1: OVERVIEW DASHBOARD */}
        {/* ========================================== */}
        {activeTab === "overview" && (
          <div style={{ display: "grid", gap: 28 }}>
            {/* Quick Metrics Widgets Grid */}
            <section style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 20 }}>
              {[
                { label: "Verified Today", value: stats.present_today, meta: "Checked-in individuals today", color: "var(--success)" },
                { label: "Checked-Out Today", value: checkedOutCount, meta: "Checkout logs recorded", color: "var(--info)" },
                { label: "Currently Present", value: presentNowCount, meta: "Users currently on campus", color: "var(--primary)" },
                { label: "Flagged Today", value: stats.rejected_today, meta: "GPS/Boundary mismatch alerts", color: "var(--danger)" },
                { label: "Not Checked-In", value: notCheckedInCount, meta: "Unchecked whitelisted users", color: "var(--text-secondary)" }
              ].map((item, idx) => (
                <article key={idx} className="dashboard-card" style={{ position: "relative", overflow: "hidden" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 8 }}>
                    <span style={{ color: "var(--text-secondary)", fontSize: 12.5, fontWeight: 700, textTransform: "uppercase", letterSpacing: 0.8 }}>
                      {item.label}
                    </span>
                  </div>
                  <div style={{ fontSize: 36, fontWeight: 800, color: "var(--text-primary)", lineHeight: 1.1 }}>
                    {item.value}
                  </div>
                  <p style={{ margin: "8px 0 0", color: "var(--text-secondary)", fontSize: 13 }}>
                    {item.meta}
                  </p>
                  <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, height: 4, background: item.color }} />
                </article>
              ))}
            </section>

            {/* Core Visualization & Table Grid */}
            <section style={{ display: "grid", gridTemplateColumns: "2.4fr 1.1fr", gap: 24, alignItems: "start" }}>
              {/* Live Attendance Logs container */}
              <article className="dashboard-card" style={{ overflow: "hidden", padding: 0 }}>
                <div style={{ padding: 24, borderBottom: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 16 }}>
                  <div>
                    <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Live Attendance Logs</h2>
                    <p style={{ margin: "4px 0 0", color: "var(--text-secondary)", fontSize: 13 }}>Real-time verification telemetry from geofences.</p>
                  </div>

                  <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
                    {/* Search Field */}
                    <div style={{ position: "relative" }}>
                      <input
                        type="text"
                        placeholder="Search logs..."
                        value={overviewSearch}
                        onChange={(e) => { setOverviewSearch(e.target.value); setOverviewPage(1); }}
                        className="custom-input"
                        style={{ paddingLeft: 34, fontSize: 13, height: 38, width: 180 }}
                      />
                      <span style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: "var(--text-secondary)", display: "grid", placeItems: "center" }}>
                        <SearchIcon />
                      </span>
                    </div>

                    {/* Filter controllers */}
                    <div style={{ display: "inline-flex", background: "var(--table-header-bg)", border: "1px solid var(--border-color)", padding: 3, borderRadius: 10 }}>
                      {[
                        { id: "all", label: "All" },
                        { id: "verified", label: "Verified" },
                        { id: "rejected", label: "Flagged" },
                        { id: "checkedout", label: "Out" }
                      ].map(opt => (
                        <button
                          key={opt.id}
                          onClick={() => { setLogFilter(opt.id); setOverviewPage(1); }}
                          style={{
                            padding: "6px 12px",
                            border: "none",
                            borderRadius: 8,
                            fontSize: 12,
                            fontWeight: 600,
                            cursor: "pointer",
                            background: logFilter === opt.id ? "var(--bg-card)" : "transparent",
                            color: logFilter === opt.id ? "var(--primary)" : "var(--text-secondary)",
                            boxShadow: logFilter === opt.id ? "var(--shadow-sm)" : "none",
                            transition: "all var(--transition-speed) ease"
                          }}
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                <div style={{ overflowX: "auto" }}>
                  {paginatedOverviewLogs.length === 0 ? (
                    <div style={{ padding: 40, textAlign: "center", color: "var(--text-secondary)" }}>
                      <p style={{ fontSize: 24, margin: "0 0 8px" }}>📭</p>
                      <p style={{ margin: 0, fontWeight: 500 }}>No matching attendance records found.</p>
                    </div>
                  ) : (
                    <table className="custom-table">
                      <thead>
                        <tr>
                          <th>Employee</th>
                          <th>Geofence Area</th>
                          <th>Check-In (GPS Details)</th>
                          <th>Check-Out (GPS Details)</th>
                          <th style={{ textAlign: "right" }}>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {paginatedOverviewLogs.map((log) => {
                          const isVerified = log.status.toLowerCase() === "verified";
                          const isCheckedOut = log.check_out_time !== null;
                          return (
                            <tr key={log.id}>
                              <td>
                                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                                  <div style={{
                                    width: 36,
                                    height: 36,
                                    borderRadius: 10,
                                    background: "var(--primary-light)",
                                    color: "var(--primary)",
                                    display: "grid",
                                    placeItems: "center",
                                    fontWeight: 700,
                                    fontSize: 13
                                  }}>
                                    {log.user_name.split(" ").map(n => n[0]).join("").substring(0, 2).toUpperCase()}
                                  </div>
                                  <div>
                                    <div style={{ fontWeight: 600, color: "var(--text-primary)" }}>{log.user_name}</div>
                                    <div style={{ color: "var(--text-secondary)", fontSize: 11.5 }}>{log.user_email}</div>
                                  </div>
                                </div>
                              </td>
                              <td>
                                <span style={{ fontWeight: 500 }}>{log.location_name}</span>
                              </td>
                              <td>
                                <div style={{ fontWeight: 600 }}>{formatTime(log.check_in_time)}</div>
                                <div style={{ color: "var(--text-secondary)", fontSize: 11, marginTop: 2 }}>
                                  {log.calculated_distance.toFixed(1)}m away ({log.device_latitude.toFixed(5)}, {log.device_longitude.toFixed(5)})
                                </div>
                              </td>
                              <td>
                                {isCheckedOut ? (
                                  <>
                                    <div style={{ fontWeight: 600 }}>{formatTime(log.check_out_time)}</div>
                                    <div style={{ color: "var(--text-secondary)", fontSize: 11, marginTop: 2 }}>
                                      {log.check_out_distance?.toFixed(1)}m away ({log.check_out_status})
                                    </div>
                                  </>
                                ) : (
                                  <span style={{ color: "var(--text-secondary)", fontSize: 12.5, fontStyle: "italic" }}>Not checked out</span>
                                )}
                              </td>
                              <td style={{ textAlign: "right" }}>
                                <span className={`status-badge ${log.status.toLowerCase()}`}>
                                  <span style={{ width: 6, height: 6, borderRadius: "50%", background: isVerified ? "var(--success)" : "var(--danger)" }} />
                                  {log.status}
                                </span>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </div>

                {/* Pagination bar */}
                {totalOverviewPages > 1 && (
                  <div style={{ padding: "16px 24px", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>
                      Showing Page <b>{overviewPage}</b> of <b>{totalOverviewPages}</b> ({filteredLogs.length} total entries)
                    </span>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button
                        onClick={() => setOverviewPage(p => Math.max(1, p - 1))}
                        disabled={overviewPage === 1}
                        className="theme-btn"
                        style={{ padding: "6px 12px", opacity: overviewPage === 1 ? 0.5 : 1, cursor: overviewPage === 1 ? "not-allowed" : "pointer" }}
                      >
                        <ChevronLeftIcon />
                        <span>Prev</span>
                      </button>
                      <button
                        onClick={() => setOverviewPage(p => Math.min(totalOverviewPages, p + 1))}
                        disabled={overviewPage === totalOverviewPages}
                        className="theme-btn"
                        style={{ padding: "6px 12px", opacity: overviewPage === totalOverviewPages ? 0.5 : 1, cursor: overviewPage === totalOverviewPages ? "not-allowed" : "pointer" }}
                      >
                        <span>Next</span>
                        <ChevronRightIcon />
                      </button>
                    </div>
                  </div>
                )}
              </article>

              {/* Right Panel: SVG Attendance Rates & Recent Events */}
              <aside style={{ display: "grid", gap: 24 }}>
                {/* Visual Chart Card */}
                <article className="dashboard-card">
                  <h3 style={{ margin: "0 0 16px", fontSize: 16, fontWeight: 700, color: "var(--text-primary)" }}>Attendance Metrics</h3>
                  <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 20 }}>
                    {/* SVG Radial Progress Ring */}
                    <div style={{ position: "relative", width: 120, height: 120 }}>
                      <svg width="120" height="120" viewBox="0 0 120 120" style={{ transform: "rotate(-90deg)" }}>
                        {/* Background circle */}
                        <circle cx="60" cy="60" r="50" fill="transparent" stroke="var(--border-color)" strokeWidth="10" />
                        {/* Foreground animated progress */}
                        <circle
                          cx="60"
                          cy="60"
                          r="50"
                          fill="transparent"
                          stroke="var(--primary)"
                          strokeWidth="10"
                          strokeDasharray={2 * Math.PI * 50}
                          strokeDashoffset={2 * Math.PI * 50 * (1 - attendanceRate / 100)}
                          strokeLinecap="round"
                          style={{ transition: "stroke-dashoffset 0.8s ease" }}
                        />
                      </svg>
                      <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center" }}>
                        <span style={{ fontSize: 22, fontWeight: 800, color: "var(--text-primary)" }}>{attendanceRate}%</span>
                        <span style={{ fontSize: 10, color: "var(--text-secondary)", fontWeight: 600, textTransform: "uppercase" }}>Present</span>
                      </div>
                    </div>
                    <p style={{ margin: "4px 0 0", color: "var(--text-secondary)", fontSize: 12.5, textAlign: "center", lineHeight: 1.4 }}>
                      Indicates today's overall check-in rate: <b>{stats.present_today}</b> out of <b>{stats.total_employees}</b> faculty members successfully verified.
                    </p>

                    <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 10 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}>
                        <span style={{ color: "var(--text-secondary)", fontWeight: 500 }}>Target Threshold</span>
                        <span style={{ color: "var(--success-text)", fontWeight: 700 }}>85.0%</span>
                      </div>
                      {/* Horizontal progress bar comparing Checked-Out ratio */}
                      <div style={{ width: "100%", height: 6, background: "var(--border-color)", borderRadius: 3, overflow: "hidden" }}>
                        <div style={{ height: "100%", background: "var(--success)", width: `${Math.min(100, Math.round((checkedOutCount / (stats.present_today || 1)) * 100))}%`, borderRadius: 3 }} />
                      </div>
                      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11.5, color: "var(--text-secondary)" }}>
                        <span>Checkout Rate</span>
                        <span>{stats.present_today > 0 ? Math.round((checkedOutCount / stats.present_today) * 100) : 0}% compliance</span>
                      </div>
                    </div>
                  </div>
                </article>

                {/* Telemetry Events Card */}
                <article className="dashboard-card" style={{ padding: 24 }}>
                  <h3 style={{ margin: "0 0 4px", fontSize: 16, fontWeight: 700, color: "var(--text-primary)" }}>Telemetry Events</h3>
                  <p style={{ margin: "0 0 20px", color: "var(--text-secondary)", fontSize: 12.5 }}>Real-time geofence validations.</p>

                  <div style={{ display: "grid", gap: 18 }}>
                    {logs.slice(0, 5).map((log, idx) => {
                      const isVerified = log.status.toLowerCase() === "verified";
                      return (
                        <div key={log.id || idx} style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
                          <span style={{
                            width: 8,
                            height: 8,
                            borderRadius: "50%",
                            marginTop: 5,
                            background: isVerified ? "var(--success)" : "var(--danger)",
                            boxShadow: `0 0 0 5px ${isVerified ? "var(--success-light)" : "var(--danger-light)"}`
                          }} />
                          <div style={{ flex: 1 }}>
                            <div style={{ color: "var(--text-primary)", fontWeight: 600, fontSize: 13 }}>
                              {log.user_name} check-in {isVerified ? "validated" : "rejected"}
                            </div>
                            <div style={{ color: "var(--text-secondary)", fontSize: 11.5, marginTop: 2 }}>
                              Site: {log.location_name} ({log.calculated_distance.toFixed(1)}m away)
                            </div>
                            <div style={{ color: "var(--text-secondary)", fontSize: 10.5, marginTop: 4 }}>
                              {formatTime(log.check_in_time)}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                    {logs.length === 0 && (
                      <p style={{ color: "var(--text-secondary)", fontSize: 13, margin: 0, textAlign: "center" }}>No recent events logged.</p>
                    )}
                  </div>
                </article>
              </aside>
            </section>
          </div>
        )}

        {/* ========================================== */}
        {/* TAB 2: EMPLOYEE DIRECTORY */}
        {/* ========================================== */}
        {activeTab === "employees" && !viewingEmployeeId && (
          <article className="dashboard-card" style={{ padding: 0, overflow: "hidden" }}>
            {/* Filter control bar */}
            <div style={{ padding: 24, borderBottom: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 16 }}>
              <div>
                <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Employee Directory</h2>
                <p style={{ margin: "4px 0 0", color: "var(--text-secondary)", fontSize: 13 }}>Search, filter, and manage institutional staff profiles.</p>
              </div>

              <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
                {/* Add employee button */}
                <button
                  onClick={() => setAddEmployeeOpen(true)}
                  className="theme-btn"
                  style={{ background: "var(--primary)", color: "white", border: "none", height: 38, padding: "0 16px", fontWeight: 600, fontSize: 13, display: "flex", gap: 6, alignItems: "center" }}
                >
                  <UsersIcon />
                  <span>Add User/Admin</span>
                </button>

                {/* Role filter */}
                <select
                  value={dirRoleFilter}
                  onChange={(e) => { setDirRoleFilter(e.target.value); setEmployeePage(1); }}
                  className="custom-input"
                  style={{ height: 38, padding: "0 12px", background: "var(--bg-card)", fontSize: 13 }}
                >
                  <option value="all">All Roles</option>
                  <option value="employee">Employee</option>
                  <option value="admin">Admin</option>
                </select>

                {/* Biometric filter */}
                <select
                  value={dirBioFilter}
                  onChange={(e) => { setDirBioFilter(e.target.value); setEmployeePage(1); }}
                  className="custom-input"
                  style={{ height: 38, padding: "0 12px", background: "var(--bg-card)", fontSize: 13 }}
                >
                  <option value="all">All Biometrics</option>
                  <option value="face">Face Registered</option>
                  <option value="fingerprint">Fingerprint Registered</option>
                  <option value="both">Both Active</option>
                  <option value="none">Pending Setup</option>
                </select>

                {/* Search query box */}
                <div style={{ position: "relative", minWidth: 260 }}>
                  <input
                    type="text"
                    placeholder="Search name or email..."
                    value={searchQuery}
                    onChange={(e) => { setSearchQuery(e.target.value); setEmployeePage(1); }}
                    className="custom-input"
                    style={{ width: "100%", paddingLeft: 34, height: 38, fontSize: 13 }}
                  />
                  <span style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: "var(--text-secondary)", display: "grid", placeItems: "center" }}>
                    <SearchIcon />
                  </span>
                  {searchQuery && (
                    <button
                      onClick={() => setSearchQuery("")}
                      style={{
                        position: "absolute",
                        right: 12,
                        top: "50%",
                        transform: "translateY(-50%)",
                        background: "none",
                        border: "none",
                        cursor: "pointer",
                        color: "var(--text-secondary)",
                        fontWeight: "bold",
                        fontSize: 12
                      }}
                    >
                      ✕
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Employee Directory grid list */}
            <div style={{ overflowX: "auto" }}>
              {paginatedEmployees.length === 0 ? (
                <div style={{ padding: 40, textAlign: "center", color: "var(--text-secondary)" }}>
                  <p style={{ fontSize: 24, margin: "0 0 8px" }}>🔍</p>
                  <p style={{ margin: 0, fontWeight: 500 }}>No profiles found matching search constraints.</p>
                </div>
              ) : (
                <table className="custom-table">
                  <thead>
                    <tr>
                      <th>Employee Name</th>
                      <th>Verified Email</th>
                      <th>Auth Domain</th>
                      <th>Biometrics</th>
                      <th>Joined Date</th>
                      <th>System Role</th>
                      <th style={{ textAlign: "right" }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedEmployees.map((emp) => {
                      const isAdmin = emp.role.toLowerCase() === "admin";
                      return (
                        <tr key={emp.id}>
                          <td>
                            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                              <div style={{
                                width: 38,
                                height: 38,
                                borderRadius: "50%",
                                background: isAdmin ? "linear-gradient(135deg, var(--primary-light), var(--bg-app))" : "var(--table-row-hover)",
                                color: isAdmin ? "var(--primary)" : "var(--text-secondary)",
                                display: "grid",
                                placeItems: "center",
                                fontWeight: 700,
                                fontSize: 13
                              }}>
                                {emp.full_name.split(" ").map(n => n[0]).join("").substring(0, 2).toUpperCase()}
                              </div>
                              <div>
                                <div style={{ fontWeight: 600, color: "var(--text-primary)" }}>{emp.full_name}</div>
                                <div style={{ color: "var(--text-secondary)", fontSize: 11, fontFamily: "monospace" }}>{emp.id}</div>
                              </div>
                            </div>
                          </td>
                          <td>
                            <span style={{ color: "var(--text-primary)" }}>{emp.email}</span>
                          </td>
                          <td>
                            <span style={{
                              display: "inline-flex",
                              padding: "4px 8px",
                              borderRadius: 6,
                              background: "var(--table-header-bg)",
                              border: "1px solid var(--border-color)",
                              color: "var(--text-secondary)",
                              fontSize: 12,
                              fontWeight: 500
                            }}>
                              🌐 {emp.domain_name || "N/A"}
                            </span>
                          </td>
                          <td>
                            <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                              <div
                                title={`Face Profile: ${emp.face_registered ? "Registered & Verified" : "Not Configured"}`}
                                style={{
                                  display: "flex",
                                  alignItems: "center",
                                  gap: 5,
                                  padding: "4px 8px",
                                  borderRadius: 8,
                                  background: emp.face_registered ? "var(--success-light)" : "var(--badge-bg-neutral)",
                                  color: emp.face_registered ? "var(--success-text)" : "var(--text-secondary)",
                                  fontSize: 12,
                                  fontWeight: 600,
                                  border: `1px solid ${emp.face_registered ? "var(--success)" : "var(--border-color)"}25`
                                }}
                              >
                                <FaceIcon active={emp.face_registered} />
                                <span>Face</span>
                              </div>
                              <div
                                title={`Fingerprint Key: ${emp.fingerprint_registered ? "Registered & Verified" : "Not Configured"}`}
                                style={{
                                  display: "flex",
                                  alignItems: "center",
                                  gap: 5,
                                  padding: "4px 8px",
                                  borderRadius: 8,
                                  background: emp.fingerprint_registered ? "var(--success-light)" : "var(--badge-bg-neutral)",
                                  color: emp.fingerprint_registered ? "var(--success-text)" : "var(--text-secondary)",
                                  fontSize: 12,
                                  fontWeight: 600,
                                  border: `1px solid ${emp.fingerprint_registered ? "var(--success)" : "var(--border-color)"}25`
                                }}
                              >
                                <FingerprintIcon active={emp.fingerprint_registered} />
                                <span>Finger</span>
                              </div>
                            </div>
                          </td>
                          <td>
                            <span style={{ color: "var(--text-secondary)", fontSize: 13.5 }}>
                              {formatDate(emp.created_at)}
                            </span>
                          </td>
                          <td>
                            <span style={{
                              display: "inline-block",
                              padding: "4px 10px",
                              borderRadius: 8,
                              fontSize: 12,
                              fontWeight: 700,
                              background: isAdmin ? "var(--primary-light)" : "var(--badge-bg-neutral)",
                              color: isAdmin ? "var(--primary)" : "var(--text-secondary)"
                            }}>
                              {emp.role.toUpperCase()}
                            </span>
                          </td>
                          <td style={{ textAlign: "right" }}>
                            <div style={{ display: "inline-flex", gap: 8 }}>
                              <button
                                onClick={() => setViewingEmployeeId(emp.id)}
                                className="theme-btn"
                                style={{ padding: "6px 12px", fontSize: 12.5 }}
                              >
                                View Stats
                              </button>
                              <button
                                onClick={() => {
                                  setEditingEmployee(emp);
                                  setEditForm({
                                    full_name: emp.full_name,
                                    email: emp.email,
                                    phone: emp.phone || "",
                                    role: emp.role || "employee"
                                  });
                                }}
                                className="theme-btn"
                                style={{ padding: "6px", display: "grid", placeItems: "center" }}
                                title="Edit Profile"
                              >
                                <EditIcon />
                              </button>
                              <button
                                onClick={() => handleDeleteEmployee(emp)}
                                className="theme-btn"
                                style={{ padding: "6px", display: "grid", placeItems: "center", background: "var(--danger-light)", color: "var(--danger)" }}
                                title="Delete Profile"
                              >
                                <TrashIcon />
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            {/* Pagination bar */}
            {totalEmployeePages > 1 && (
              <div style={{ padding: "16px 24px", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>
                  Showing Page <b>{employeePage}</b> of <b>{totalEmployeePages}</b> ({filteredEmployees.length} total entries)
                </span>
                <div style={{ display: "flex", gap: 8 }}>
                  <button
                    onClick={() => setEmployeePage(p => Math.max(1, p - 1))}
                    disabled={employeePage === 1}
                    className="theme-btn"
                    style={{ padding: "6px 12px", opacity: employeePage === 1 ? 0.5 : 1, cursor: employeePage === 1 ? "not-allowed" : "pointer" }}
                  >
                    <ChevronLeftIcon />
                    <span>Prev</span>
                  </button>
                  <button
                    onClick={() => setEmployeePage(p => Math.min(totalEmployeePages, p + 1))}
                    disabled={employeePage === totalEmployeePages}
                    className="theme-btn"
                    style={{ padding: "6px 12px", opacity: employeePage === totalEmployeePages ? 0.5 : 1, cursor: employeePage === totalEmployeePages ? "not-allowed" : "pointer" }}
                  >
                    <span>Next</span>
                    <ChevronRightIcon />
                  </button>
                </div>
              </div>
            )}
          </article>
        )}

        {/* ========================================== */}
        {/* SUB-PAGE: INDIVIDUAL EMPLOYEE STATS */}
        {/* ========================================== */}
        {activeTab === "employees" && viewingEmployeeId && activeEmployee && (
          <div style={{ display: "grid", gap: 28 }}>
            {/* Top Navigation Back Action */}
            <div style={{ display: "flex", justifyContent: "flex-start" }}>
              <button
                onClick={() => setViewingEmployeeId(null)}
                className="theme-btn"
                style={{ padding: "8px 16px" }}
              >
                <ChevronLeftIcon />
                <span>Back to Employee Directory</span>
              </button>
            </div>

            <section style={{ display: "grid", gridTemplateColumns: "1fr 2.5fr", gap: 24, alignItems: "start" }}>
              {/* Profile Card left */}
              <article className="dashboard-card" style={{ display: "grid", gap: 20, placeContent: "start" }}>
                <div style={{ textAlign: "center", borderBottom: "1px solid var(--border-color)", paddingBottom: 20 }}>
                  <div style={{
                    width: 72,
                    height: 72,
                    borderRadius: "50%",
                    background: "var(--primary-light)",
                    color: "var(--primary)",
                    display: "grid",
                    placeItems: "center",
                    fontWeight: 700,
                    fontSize: 24,
                    margin: "0 auto 14px"
                  }}>
                    {activeEmployee.full_name.split(" ").map(n => n[0]).join("").substring(0, 2).toUpperCase()}
                  </div>
                  <h3 style={{ margin: "0 0 4px", fontSize: 18, color: "var(--text-primary)" }}>{activeEmployee.full_name}</h3>
                  <span style={{
                    display: "inline-block",
                    padding: "3px 10px",
                    borderRadius: 6,
                    fontSize: 11,
                    fontWeight: 700,
                    background: activeEmployee.role.toLowerCase() === "admin" ? "var(--primary-light)" : "var(--badge-bg-neutral)",
                    color: activeEmployee.role.toLowerCase() === "admin" ? "var(--primary)" : "var(--text-secondary)"
                  }}>
                    {activeEmployee.role.toUpperCase()}
                  </span>
                </div>

                <div style={{ display: "grid", gap: 14, fontSize: 13.5 }}>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <MailIcon />
                    <span style={{ color: "var(--text-primary)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{activeEmployee.email}</span>
                  </div>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <PhoneIcon />
                    <span style={{ color: "var(--text-primary)" }}>{activeEmployee.phone || "No phone added"}</span>
                  </div>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <CalendarIcon />
                    <span style={{ color: "var(--text-secondary)" }}>Joined: {formatDate(activeEmployee.created_at)}</span>
                  </div>
                </div>

                {/* Biometric Status indicators */}
                <div style={{ borderTop: "1px solid var(--border-color)", paddingTop: 16 }}>
                  <h4 style={{ margin: "0 0 12px", fontSize: 13, color: "var(--text-secondary)", fontWeight: 700, textTransform: "uppercase", letterSpacing: 0.6 }}>
                    Biometric Enrolment
                  </h4>
                  <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", fontSize: 13 }}>
                      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                        <FaceIcon active={activeEmployee.face_registered} />
                        <span>Face Scan Profile</span>
                      </div>
                      <span style={{ fontWeight: 700, color: activeEmployee.face_registered ? "var(--success)" : "var(--text-secondary)" }}>
                        {activeEmployee.face_registered ? "Registered" : "Pending"}
                      </span>
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", fontSize: 13 }}>
                      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                        <FingerprintIcon active={activeEmployee.fingerprint_registered} />
                        <span>Fingerprint Key</span>
                      </div>
                      <span style={{ fontWeight: 700, color: activeEmployee.fingerprint_registered ? "var(--success)" : "var(--text-secondary)" }}>
                        {activeEmployee.fingerprint_registered ? "Registered" : "Pending"}
                      </span>
                    </div>
                  </div>
                </div>
              </article>

              {/* Stats & personal logs right */}
              <div style={{ display: "grid", gap: 24 }}>
                {/* Stats Widgets Grid */}
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 18 }}>
                  {/* Streak Card */}
                  <article className="dashboard-card" style={{ display: "flex", alignItems: "center", gap: 16 }}>
                    <div style={{ width: 44, height: 44, borderRadius: 12, background: "rgba(239, 68, 68, 0.1)", display: "grid", placeItems: "center" }}>
                      <FlameIcon className="streak-flame" />
                    </div>
                    <div>
                      <div style={{ color: "var(--text-secondary)", fontSize: 12, fontWeight: 700, textTransform: "uppercase" }}>Current Streak</div>
                      <div style={{ fontSize: 24, fontWeight: 800, color: "var(--text-primary)" }}>
                        {calculateStreak(employeeLogs)} Days
                      </div>
                    </div>
                  </article>

                  {/* Monthly Attendance */}
                  <article className="dashboard-card" style={{ display: "flex", alignItems: "center", gap: 16 }}>
                    <div style={{ width: 44, height: 44, borderRadius: 12, background: "var(--primary-light)", display: "grid", placeItems: "center", color: "var(--primary)" }}>
                      <CalendarIcon />
                    </div>
                    <div>
                      <div style={{ color: "var(--text-secondary)", fontSize: 12, fontWeight: 700, textTransform: "uppercase" }}>Month Attendance</div>
                      <div style={{ fontSize: 24, fontWeight: 800, color: "var(--text-primary)" }}>
                        {calculateMonthlyStats(employeeLogs).rate}%
                      </div>
                      <div style={{ fontSize: 11, color: "var(--text-secondary)", marginTop: 2 }}>
                        {calculateMonthlyStats(employeeLogs).verifiedCount} present days
                      </div>
                    </div>
                  </article>

                  {/* Checkout Compliance */}
                  <article className="dashboard-card" style={{ display: "flex", alignItems: "center", gap: 16 }}>
                    <div style={{ width: 44, height: 44, borderRadius: 12, background: "var(--success-light)", display: "grid", placeItems: "center", color: "var(--success)" }}>
                      <CheckIcon />
                    </div>
                    <div>
                      <div style={{ color: "var(--text-secondary)", fontSize: 12, fontWeight: 700, textTransform: "uppercase" }}>Checkout Rate</div>
                      <div style={{ fontSize: 24, fontWeight: 800, color: "var(--text-primary)" }}>
                        {(() => {
                          const verifiedLogs = employeeLogs.filter(log => log.status.toLowerCase() === "verified");
                          if (verifiedLogs.length === 0) return 0;
                          const completed = verifiedLogs.filter(log => log.check_out_time !== null).length;
                          return Math.round((completed / verifiedLogs.length) * 100);
                        })()}%
                      </div>
                    </div>
                  </article>
                </div>

                {/* History table */}
                <article className="dashboard-card" style={{ padding: 0, overflow: "hidden" }}>
                  <div style={{ padding: 20, borderBottom: "1px solid var(--border-color)" }}>
                    <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "var(--text-primary)" }}>Personal Attendance History</h3>
                  </div>

                  <div style={{ overflowX: "auto" }}>
                    {loadingEmpLogs ? (
                      <div style={{ padding: 40, textAlign: "center", color: "var(--text-secondary)" }}>
                        <div style={{ width: 28, height: 28, border: "3px solid var(--border-input)", borderTop: "3px solid var(--primary)", borderRadius: "50%", animation: "spin 1s linear infinite", margin: "0 auto 12px" }} />
                        <p style={{ margin: 0, fontSize: 13 }}>Loading attendance logs...</p>
                      </div>
                    ) : employeeLogs.length === 0 ? (
                      <div style={{ padding: 40, textAlign: "center", color: "var(--text-secondary)" }}>
                        <p style={{ fontSize: 24, margin: "0 0 8px" }}>📭</p>
                        <p style={{ margin: 0, fontSize: 13.5 }}>No attendance records logged for this user.</p>
                      </div>
                    ) : (
                      <table className="custom-table">
                        <thead>
                          <tr>
                            <th>Check-In Date</th>
                            <th>Boundary Site</th>
                            <th>Check-In Time</th>
                            <th>Check-Out Time</th>
                            <th>GPS Accuracy</th>
                            <th style={{ textAlign: "right" }}>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {employeeLogs
                            .slice((statsPage - 1) * 5, statsPage * 5)
                            .map((log) => {
                              const isVerified = log.status.toLowerCase() === "verified";
                              return (
                                <tr key={log.id}>
                                  <td style={{ fontWeight: 600 }}>{formatDate(log.check_in_time)}</td>
                                  <td>{log.location_name}</td>
                                  <td>{formatTime(log.check_in_time)}</td>
                                  <td>{log.check_out_time ? formatTime(log.check_out_time) : <span style={{ fontStyle: "italic", color: "var(--text-secondary)", fontSize: 12 }}>Not recorded</span>}</td>
                                  <td style={{ fontSize: 12 }}>
                                    Check-In: {log.calculated_distance.toFixed(1)}m
                                    {log.check_out_time && ` / Check-Out: ${log.calculated_distance.toFixed(1)}m`}
                                  </td>
                                  <td style={{ textAlign: "right" }}>
                                    <span className={`status-badge ${log.status.toLowerCase()}`}>
                                      {log.status}
                                    </span>
                                  </td>
                                </tr>
                              );
                            })}
                        </tbody>
                      </table>
                    )}
                  </div>

                  {/* Micro pagination for details */}
                  {!loadingEmpLogs && employeeLogs.length > 5 && (
                    <div style={{ padding: "12px 20px", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: 12.5, color: "var(--text-secondary)" }}>
                        Showing Page {statsPage} of {Math.ceil(employeeLogs.length / 5)}
                      </span>
                      <div style={{ display: "flex", gap: 6 }}>
                        <button
                          onClick={() => setStatsPage(p => Math.max(1, p - 1))}
                          disabled={statsPage === 1}
                          className="theme-btn"
                          style={{ padding: "4px 8px", fontSize: 12, opacity: statsPage === 1 ? 0.5 : 1 }}
                        >
                          Prev
                        </button>
                        <button
                          onClick={() => setStatsPage(p => Math.min(Math.ceil(employeeLogs.length / 5), p + 1))}
                          disabled={statsPage === Math.ceil(employeeLogs.length / 5)}
                          className="theme-btn"
                          style={{ padding: "4px 8px", fontSize: 12, opacity: statsPage === Math.ceil(employeeLogs.length / 5) ? 0.5 : 1 }}
                        >
                          Next
                        </button>
                      </div>
                    </div>
                  )}
                </article>
              </div>
            </section>
          </div>
        )}

        {/* ========================================== */}
        {/* TAB 3: CAMPUS BOUNDARIES & GEOMAPS */}
        {/* ========================================== */}
        {activeTab === "boundaries" && (
          <div style={{ display: "grid", gridTemplateColumns: "1.2fr 1.6fr 1.2fr", gap: 24, alignItems: "start", height: "calc(100vh - 200px)", overflow: "hidden" }}>
            {/* List panel left */}
            <section style={{ display: "grid", gap: 16, maxHeight: "100%", overflowY: "auto", paddingRight: 8 }}>
              <div style={{ padding: "4px 0" }}>
                <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Configured Sites</h2>
                <p style={{ margin: "4px 0 0", color: "var(--text-secondary)", fontSize: 13 }}>List of whitelisted tracking locations.</p>
              </div>

              {boundaries.map((b) => (
                <article
                  key={b.id}
                  className="dashboard-card"
                  style={{
                    padding: 20,
                    display: "grid",
                    gap: 12,
                    borderLeft: `4px solid ${b.is_active ? "var(--success)" : "var(--danger)"}`
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 10 }}>
                    <div>
                      <h3 style={{ margin: 0, fontSize: 15.5, fontWeight: 700, color: "var(--text-primary)" }}>{b.location_name}</h3>
                      <span style={{
                        fontSize: 10,
                        padding: "2px 8px",
                        borderRadius: 8,
                        fontWeight: 700,
                        marginTop: 4,
                        display: "inline-block",
                        background: b.is_active ? "var(--success-light)" : "var(--badge-bg-neutral)",
                        color: b.is_active ? "var(--success-text)" : "var(--text-secondary)"
                      }}>
                        {b.is_active ? "ACTIVE" : "DISABLED"}
                      </span>
                    </div>

                    <div style={{ display: "flex", gap: 6 }}>
                      <button
                        onClick={() => handleEditBoundaryClick(b)}
                        className="theme-btn"
                        style={{ padding: 6, display: "grid", placeItems: "center" }}
                        title="Edit Boundary"
                      >
                        <EditIcon />
                      </button>
                      <button
                        onClick={() => handleDeleteBoundary(b.id)}
                        className="theme-btn"
                        style={{ padding: 6, color: "var(--danger)", display: "grid", placeItems: "center" }}
                        title="Delete Boundary"
                      >
                        <TrashIcon />
                      </button>
                    </div>
                  </div>

                  <div style={{ display: "grid", gap: 4, fontSize: 12.5, color: "var(--text-secondary)" }}>
                    <div>📍 <b>Latitude:</b> {b.center_latitude.toFixed(6)}</div>
                    <div>📍 <b>Longitude:</b> {b.center_longitude.toFixed(6)}</div>
                    <div>⭕ <b>Radius:</b> {b.radius_meters} meters</div>
                  </div>

                  <label style={{ display: "flex", alignItems: "center", gap: 8, cursor: "pointer", fontSize: 12.5, fontWeight: 600, marginTop: 4 }}>
                    <input
                      type="checkbox"
                      checked={b.is_active}
                      onChange={() => handleToggleBoundary(b.id, b.is_active)}
                      style={{ width: 16, height: 16, cursor: "pointer", accentColor: "var(--primary)" }}
                    />
                    <span>Active Tracking</span>
                  </label>
                </article>
              ))}

              {boundaries.length === 0 && (
                <div style={{ padding: 40, textAlign: "center", background: "var(--bg-card)", borderRadius: 20, border: "1px solid var(--border-color)" }}>
                  <p style={{ fontSize: 24, margin: "0 0 8px" }}>🌐</p>
                  <p style={{ margin: 0, fontSize: 13.5, color: "var(--text-secondary)" }}>No locations configured yet.</p>
                </div>
              )}
            </section>

            {/* Central Interactive Map column */}
            <article className="dashboard-card" style={{ padding: 16, display: "flex", flexDirection: "column", gap: 14, height: "100%" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "var(--text-primary)" }}>Geofence Map Overview</h3>
                <span style={{ fontSize: 11.5, color: "var(--text-secondary)", fontWeight: 600 }}>
                  Green = Active / Red = Inactive
                </span>
              </div>

              {/* Map holder element */}
              <div
                ref={mapContainerRef}
                style={{
                  flex: 1,
                  minHeight: 300,
                  width: "100%",
                  borderRadius: 14,
                  border: "1px solid var(--border-input)",
                  background: "var(--table-header-bg)",
                  zIndex: 1
                }}
              />
            </article>

            {/* Config Form Right Column */}
            <aside className="dashboard-card" style={{ maxHeight: "100%", overflowY: "auto" }}>
              <h3 style={{ margin: "0 0 4px", fontSize: 17, fontWeight: 700, color: "var(--text-primary)" }}>
                {editingBoundaryId ? "Edit Geofence Boundary" : "Add Geofence Boundary"}
              </h3>
              <p style={{ margin: "0 0 20px", color: "var(--text-secondary)", fontSize: 13 }}>
                Configure coordinate center points and tracking allowances.
              </p>

              {boundarySubmitMsg.text && (
                <div style={{
                  padding: "10px 14px",
                  borderRadius: 10,
                  marginBottom: 16,
                  fontSize: 13,
                  fontWeight: 500,
                  background: boundarySubmitMsg.type === "success" ? "var(--success-light)" : "var(--danger-light)",
                  color: boundarySubmitMsg.type === "success" ? "var(--success-text)" : "var(--danger-text)",
                  border: `1px solid ${boundarySubmitMsg.type === "success" ? "var(--success)" : "var(--danger)"}40`
                }}>
                  {boundarySubmitMsg.text}
                </div>
              )}

              <form onSubmit={handleBoundarySubmit} style={{ display: "grid", gap: 16 }}>
                <div>
                  <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                    Location Name
                  </label>
                  <input
                    type="text"
                    placeholder="e.g., Main Administrative Block"
                    value={newBoundary.location_name}
                    onChange={(e) => setNewBoundary({ ...newBoundary, location_name: e.target.value })}
                    className="custom-input"
                    style={{ width: "100%" }}
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                  <div>
                    <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                      Center Latitude
                    </label>
                    <input
                      type="number"
                      step="0.000001"
                      placeholder="e.g., 11.3216"
                      value={newBoundary.center_latitude}
                      onChange={(e) => setNewBoundary({ ...newBoundary, center_latitude: e.target.value })}
                      className="custom-input"
                      style={{ width: "100%" }}
                    />
                  </div>

                  <div>
                    <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                      Center Longitude
                    </label>
                    <input
                      type="number"
                      step="0.000001"
                      placeholder="e.g., 75.9336"
                      value={newBoundary.center_longitude}
                      onChange={(e) => setNewBoundary({ ...newBoundary, center_longitude: e.target.value })}
                      className="custom-input"
                      style={{ width: "100%" }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                    Radius (meters)
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    placeholder="e.g., 70"
                    value={newBoundary.radius_meters}
                    onChange={(e) => setNewBoundary({ ...newBoundary, radius_meters: e.target.value })}
                    className="custom-input"
                    style={{ width: "100%" }}
                  />
                </div>

                <div style={{ display: "flex", flexDirection: "column", gap: 10, marginTop: 8 }}>
                  <button
                    type="submit"
                    className="theme-btn"
                    style={{
                      background: "var(--primary)",
                      color: "white",
                      border: "none",
                      padding: "12px",
                      justifyContent: "center",
                      fontWeight: 700,
                      boxShadow: "0 8px 16px var(--primary-light)"
                    }}
                  >
                    {editingBoundaryId ? "Save Geofence Changes" : "Register Geofence Area"}
                  </button>

                  {editingBoundaryId && (
                    <button
                      type="button"
                      onClick={handleCancelBoundaryEdit}
                      className="theme-btn"
                      style={{ justifyContent: "center" }}
                    >
                      Cancel Edit
                    </button>
                  )}
                </div>
              </form>
            </aside>
          </div>
        )}

        {/* ========================================== */}
        {/* TAB 4: BIOMETRIC UPDATE REQUESTS */}
        {/* ========================================== */}
        {activeTab === "biometric_requests" && (
          <article className="dashboard-card" style={{ padding: 0, overflow: "hidden" }}>
            {/* Filter toolbar */}
            <div style={{ padding: 24, borderBottom: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 16 }}>
              <div>
                <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Biometric Update Requests</h2>
                <p style={{ margin: "4px 0 0", color: "var(--text-secondary)", fontSize: 13 }}>Review and approve user requests to register/reset Face profiles or Fingerprint keys.</p>
              </div>

              {/* Filtering components */}
              <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
                {/* Status selector tabs */}
                <div style={{ display: "inline-flex", background: "var(--table-header-bg)", border: "1px solid var(--border-color)", padding: 3, borderRadius: 10 }}>
                  {[
                    { id: "pending", label: "Pending" },
                    { id: "approved", label: "Approved" },
                    { id: "rejected", label: "Rejected" },
                    { id: "all", label: "All Logs" }
                  ].map(opt => (
                    <button
                      key={opt.id}
                      onClick={() => setBioStatusFilter(opt.id)}
                      style={{
                        padding: "6px 12px",
                        border: "none",
                        borderRadius: 8,
                        fontSize: 12,
                        fontWeight: 600,
                        cursor: "pointer",
                        background: bioStatusFilter === opt.id ? "var(--bg-card)" : "transparent",
                        color: bioStatusFilter === opt.id ? "var(--primary)" : "var(--text-secondary)",
                        boxShadow: bioStatusFilter === opt.id ? "var(--shadow-sm)" : "none",
                        transition: "all var(--transition-speed) ease"
                      }}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>

                {/* Request type filter */}
                <select
                  value={bioTypeFilter}
                  onChange={(e) => setBioTypeFilter(e.target.value)}
                  className="custom-input"
                  style={{ height: 38, padding: "0 10px", background: "var(--bg-card)", fontSize: 13 }}
                >
                  <option value="all">All Types</option>
                  <option value="face">Face Scan Only</option>
                  <option value="fingerprint">Fingerprint Key</option>
                </select>

                {/* Date Sort filter */}
                <select
                  value={bioDateSort}
                  onChange={(e) => setBioDateSort(e.target.value)}
                  className="custom-input"
                  style={{ height: 38, padding: "0 10px", background: "var(--bg-card)", fontSize: 13 }}
                >
                  <option value="newest">Sort: Newest First</option>
                  <option value="oldest">Sort: Oldest First</option>
                </select>

                {/* Date Range filter */}
                <select
                  value={bioDateFilter}
                  onChange={(e) => setBioDateFilter(e.target.value)}
                  className="custom-input"
                  style={{ height: 38, padding: "0 10px", background: "var(--bg-card)", fontSize: 13 }}
                >
                  <option value="all">Any Date</option>
                  <option value="today">Today Only</option>
                  <option value="week">Past 7 Days</option>
                  <option value="month">Past 30 Days</option>
                </select>
              </div>
            </div>

            {/* Requests list table */}
            <div style={{ overflowX: "auto" }}>
              {filteredBioRequests.length === 0 ? (
                <div style={{ padding: 40, textAlign: "center", color: "var(--text-secondary)" }}>
                  <p style={{ fontSize: 24, margin: "0 0 8px" }}>📥</p>
                  <p style={{ margin: 0, fontWeight: 500 }}>No biometric requests matching filters found.</p>
                </div>
              ) : (
                <table className="custom-table">
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th>Request Type</th>
                      <th>Update Preview</th>
                      <th>Requested Date</th>
                      <th>Status</th>
                      <th style={{ textAlign: "right" }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredBioRequests.map((req) => {
                      const isFace = req.request_type === "face";
                      const isPending = req.status.toLowerCase() === "pending";
                      return (
                        <tr key={req.id}>
                          <td>
                            <div style={{ fontWeight: 600, color: "var(--text-primary)" }}>{req.user_name}</div>
                            <div style={{ color: "var(--text-secondary)", fontSize: 12 }}>{req.user_email}</div>
                          </td>
                          <td>
                            <span style={{
                              display: "inline-flex",
                              alignItems: "center",
                              gap: 6,
                              padding: "4px 10px",
                              borderRadius: 8,
                              fontSize: 12,
                              fontWeight: 700,
                              background: isFace ? "var(--primary-light)" : "var(--badge-bg-neutral)",
                              color: isFace ? "var(--primary)" : "var(--text-secondary)"
                            }}>
                              {isFace ? "Face Scan Profile" : "Fingerprint Key"}
                            </span>
                          </td>
                          <td>
                            {isFace ? (
                              req.new_face_image ? (
                                <img
                                  src={`data:image/jpeg;base64,${req.new_face_image}`}
                                  alt="Face Preview"
                                  style={{
                                    width: 60,
                                    height: 60,
                                    borderRadius: 12,
                                    objectFit: "cover",
                                    border: "1px solid var(--border-input)"
                                  }}
                                />
                              ) : (
                                <span style={{ color: "var(--text-secondary)", fontSize: 13 }}>No Image</span>
                              )
                            ) : (
                              <div style={{ fontSize: 11, fontFamily: "monospace", color: "var(--text-secondary)", maxWidth: 180, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                {req.new_public_key}
                              </div>
                            )}
                          </td>
                          <td>
                            <span style={{ color: "var(--text-secondary)", fontSize: 13.5 }}>
                              {formatDate(req.created_at)}
                            </span>
                          </td>
                          <td>
                            <span className={`status-badge ${req.status.toLowerCase()}`}>
                              {req.status}
                            </span>
                          </td>
                          <td style={{ textAlign: "right" }}>
                            {isPending ? (
                              <div style={{ display: "inline-flex", gap: 8 }}>
                                <button
                                  onClick={() => handleActionBiometricRequest(req.id, "approve")}
                                  className="theme-btn"
                                  style={{ background: "var(--success)", color: "white", border: "none", padding: "6px 12px", fontSize: 12 }}
                                >
                                  Approve
                                </button>
                                <button
                                  onClick={() => handleActionBiometricRequest(req.id, "reject")}
                                  className="theme-btn"
                                  style={{ background: "var(--danger)", color: "white", border: "none", padding: "6px 12px", fontSize: 12 }}
                                >
                                  Reject
                                </button>
                              </div>
                            ) : (
                              <span style={{ fontSize: 12.5, color: "var(--text-secondary)", fontStyle: "italic" }}>
                                Handled
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </article>
        )}

        {/* ========================================== */}
        {/* TAB 5: FEEDBACK LOGS */}
        {/* ========================================== */}
        {activeTab === "feedbacks" && (
          <article className="dashboard-card" style={{ padding: 0, overflow: "hidden" }}>
            <div style={{ padding: 24, borderBottom: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 16 }}>
              <div>
                <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>General User Feedback</h2>
                <p style={{ margin: "4px 0 0", color: "var(--text-secondary)", fontSize: 13 }}>Review feedback logs submitted by mobile application users.</p>
              </div>
              <div style={{ position: "relative" }}>
                <input
                  type="text"
                  placeholder="Search feedback..."
                  value={feedbackSearch}
                  onChange={(e) => { setFeedbackSearch(e.target.value); setFeedbackPage(1); }}
                  className="custom-input"
                  style={{ paddingLeft: 34, fontSize: 13, height: 38, width: 220 }}
                />
                <span style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: "var(--text-secondary)", display: "grid", placeItems: "center" }}>
                  <SearchIcon />
                </span>
              </div>
            </div>

            <div style={{ overflowX: "auto" }}>
              {(() => {
                const filteredFeedbacks = feedbacks.filter(fb =>
                  fb.user_name.toLowerCase().includes(feedbackSearch.toLowerCase()) ||
                  fb.user_email.toLowerCase().includes(feedbackSearch.toLowerCase()) ||
                  fb.message.toLowerCase().includes(feedbackSearch.toLowerCase())
                );
                const totalFbPages = Math.ceil(filteredFeedbacks.length / itemsPerPage) || 1;
                const paginatedFeedbacks = filteredFeedbacks.slice((feedbackPage - 1) * itemsPerPage, feedbackPage * itemsPerPage);

                if (paginatedFeedbacks.length === 0) {
                  return (
                    <div style={{ padding: 40, textAlign: "center", color: "var(--text-secondary)" }}>
                      <p style={{ fontSize: 24, margin: "0 0 8px" }}>💬</p>
                      <p style={{ margin: 0, fontWeight: 500 }}>No feedback submissions found.</p>
                    </div>
                  );
                }

                return (
                  <>
                    <table className="custom-table">
                      <thead>
                        <tr>
                          <th>User Details</th>
                          <th>Message Content</th>
                          <th style={{ textAlign: "right" }}>Submitted Date</th>
                        </tr>
                      </thead>
                      <tbody>
                        {paginatedFeedbacks.map(fb => (
                          <tr key={fb.id}>
                            <td style={{ width: "30%" }}>
                              <div style={{ fontWeight: 600, color: "var(--text-primary)" }}>{fb.user_name}</div>
                              <div style={{ color: "var(--text-secondary)", fontSize: 12 }}>{fb.user_email}</div>
                            </td>
                            <td style={{ whiteSpace: "pre-wrap", color: "var(--text-primary)", fontSize: 13.5, lineHeight: 1.5, padding: "16px 12px" }}>
                              {fb.message}
                            </td>
                            <td style={{ textAlign: "right", color: "var(--text-secondary)", fontSize: 13.5 }}>
                              <div>{formatDate(fb.created_at)}</div>
                              <div style={{ fontSize: 11, marginTop: 4 }}>{formatTime(fb.created_at)}</div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>

                    {totalFbPages > 1 && (
                      <div style={{ padding: "16px 24px", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>
                          Page {feedbackPage} of {totalFbPages}
                        </span>
                        <div style={{ display: "flex", gap: 8 }}>
                          <button
                            disabled={feedbackPage === 1}
                            onClick={() => setFeedbackPage(prev => Math.max(1, prev - 1))}
                            className="theme-btn"
                            style={{ height: 32, padding: "0 10px" }}
                          >
                            <ChevronLeftIcon /> Prev
                          </button>
                          <button
                            disabled={feedbackPage === totalFbPages}
                            onClick={() => setFeedbackPage(prev => Math.min(totalFbPages, prev + 1))}
                            className="theme-btn"
                            style={{ height: 32, padding: "0 10px" }}
                          >
                            Next <ChevronRightIcon />
                          </button>
                        </div>
                      </div>
                    )}
                  </>
                );
              })()}
            </div>
          </article>
        )}

        {/* ========================================== */}
        {/* TAB 6: SUPPORT TICKETS */}
        {/* ========================================== */}
        {activeTab === "support" && (
          <article className="dashboard-card" style={{ padding: 0, overflow: "hidden" }}>
            <div style={{ padding: 24, borderBottom: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 16 }}>
              <div>
                <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Support Tickets</h2>
                <p style={{ margin: "4px 0 0", color: "var(--text-secondary)", fontSize: 13 }}>View and reply to user support queries and assistance requests.</p>
              </div>
              <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
                {/* Status Filter */}
                <div style={{ display: "inline-flex", background: "var(--table-header-bg)", border: "1px solid var(--border-color)", padding: 3, borderRadius: 10 }}>
                  {[
                    { id: "all", label: "All Tickets" },
                    { id: "pending", label: "Pending" },
                    { id: "replied", label: "Replied" }
                  ].map(opt => (
                    <button
                      key={opt.id}
                      onClick={() => { setSupportStatusFilter(opt.id); setSupportPage(1); }}
                      style={{
                        padding: "6px 12px",
                        border: "none",
                        borderRadius: 8,
                        fontSize: 12,
                        fontWeight: 600,
                        cursor: "pointer",
                        background: supportStatusFilter === opt.id ? "var(--bg-card)" : "transparent",
                        color: supportStatusFilter === opt.id ? "var(--primary)" : "var(--text-secondary)",
                        boxShadow: supportStatusFilter === opt.id ? "var(--shadow-sm)" : "none",
                        transition: "all var(--transition-speed) ease"
                      }}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
                {/* Search Bar */}
                <div style={{ position: "relative" }}>
                  <input
                    type="text"
                    placeholder="Search tickets..."
                    value={supportSearch}
                    onChange={(e) => { setSupportSearch(e.target.value); setSupportPage(1); }}
                    className="custom-input"
                    style={{ paddingLeft: 34, fontSize: 13, height: 38, width: 180 }}
                  />
                  <span style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: "var(--text-secondary)", display: "grid", placeItems: "center" }}>
                    <SearchIcon />
                  </span>
                </div>
              </div>
            </div>

            <div style={{ overflowX: "auto" }}>
              {(() => {
                const filteredSupport = supportRequests.filter(req => {
                  const matchesSearch = req.user_name.toLowerCase().includes(supportSearch.toLowerCase()) ||
                    req.user_email.toLowerCase().includes(supportSearch.toLowerCase()) ||
                    req.message.toLowerCase().includes(supportSearch.toLowerCase());

                  if (!matchesSearch) return false;
                  if (supportStatusFilter === "all") return true;
                  return req.status.toLowerCase() === supportStatusFilter.toLowerCase();
                });
                const totalSupPages = Math.ceil(filteredSupport.length / itemsPerPage) || 1;
                const paginatedSupport = filteredSupport.slice((supportPage - 1) * itemsPerPage, supportPage * itemsPerPage);

                if (paginatedSupport.length === 0) {
                  return (
                    <div style={{ padding: 40, textAlign: "center", color: "var(--text-secondary)" }}>
                      <p style={{ fontSize: 24, margin: "0 0 8px" }}>⚓</p>
                      <p style={{ margin: 0, fontWeight: 500 }}>No support tickets matching filters found.</p>
                    </div>
                  );
                }

                return (
                  <>
                    <table className="custom-table">
                      <thead>
                        <tr>
                          <th>User Details</th>
                          <th>Request Details</th>
                          <th>Status</th>
                          <th style={{ textAlign: "right" }}>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {paginatedSupport.map(req => {
                          const isReplied = req.status.toLowerCase() === "replied";
                          return (
                            <tr key={req.id}>
                              <td style={{ width: "25%" }}>
                                <div style={{ fontWeight: 600, color: "var(--text-primary)" }}>{req.user_name}</div>
                                <div style={{ color: "var(--text-secondary)", fontSize: 12 }}>{req.user_email}</div>
                                <div style={{ fontSize: 11.5, color: "var(--text-secondary)", marginTop: 8 }}>
                                  Submitted: {formatDate(req.created_at)} at {formatTime(req.created_at)}
                                </div>
                              </td>
                              <td style={{ padding: "16px 12px" }}>
                                <div style={{ fontWeight: 500, color: "var(--text-primary)", fontSize: 13.5, whiteSpace: "pre-wrap", marginBottom: 12 }}>
                                  {req.message}
                                </div>
                                {isReplied && (
                                  <div style={{
                                    background: "var(--table-header-bg)",
                                    border: "1px solid var(--border-color)",
                                    borderRadius: 12,
                                    padding: 12,
                                    marginTop: 8
                                  }}>
                                    <div style={{ fontSize: 11, fontWeight: 700, color: "var(--success)", display: "flex", gap: 6, alignItems: "center", marginBottom: 6 }}>
                                      <CheckIcon /> ADMIN RESPONSE ({formatDate(req.replied_at)}):
                                    </div>
                                    <div style={{ fontSize: 13, color: "var(--text-secondary)", fontStyle: "italic", whiteSpace: "pre-wrap" }}>
                                      {req.reply}
                                    </div>
                                  </div>
                                )}
                              </td>
                              <td style={{ width: "12%" }}>
                                <span className={`status-badge ${req.status.toLowerCase()}`}>
                                  {req.status}
                                </span>
                              </td>
                              <td style={{ textAlign: "right", width: "12%" }}>
                                <button
                                  onClick={() => setReplyTicket(req)}
                                  className="theme-btn"
                                  style={{
                                    background: isReplied ? "transparent" : "var(--primary)",
                                    color: isReplied ? "var(--primary)" : "white",
                                    border: isReplied ? "1px solid var(--primary)" : "none",
                                    padding: "6px 14px",
                                    fontSize: 12.5,
                                    display: "inline-flex"
                                  }}
                                >
                                  {isReplied ? "Update Reply" : "Send Reply"}
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>

                    {totalSupPages > 1 && (
                      <div style={{ padding: "16px 24px", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>
                          Page {supportPage} of {totalSupPages}
                        </span>
                        <div style={{ display: "flex", gap: 8 }}>
                          <button
                            disabled={supportPage === 1}
                            onClick={() => setSupportPage(prev => Math.max(1, prev - 1))}
                            className="theme-btn"
                            style={{ height: 32, padding: "0 10px" }}
                          >
                            <ChevronLeftIcon /> Prev
                          </button>
                          <button
                            disabled={supportPage === totalSupPages}
                            onClick={() => setSupportPage(prev => Math.min(totalSupPages, prev + 1))}
                            className="theme-btn"
                            style={{ height: 32, padding: "0 10px" }}
                          >
                            Next <ChevronRightIcon />
                          </button>
                        </div>
                      </div>
                    )}
                  </>
                );
              })()}
            </div>
          </article>
        )}

        {/* ========================================== */}
        {/* TAB 7: ADMIN ACTION LOGS */}
        {/* ========================================== */}
        {activeTab === "admin_logs" && (
          <article className="dashboard-card" style={{ padding: 0, overflow: "hidden" }}>
            <div style={{ padding: 24, borderBottom: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 16 }}>
              <div>
                <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Admin Action Logs</h2>
                <p style={{ margin: "4px 0 0", color: "var(--text-secondary)", fontSize: 13 }}>System logs tracking create, update, and delete actions performed by administrators.</p>
              </div>
              <div style={{ position: "relative" }}>
                <input
                  type="text"
                  placeholder="Search logs..."
                  value={adminLogSearch}
                  onChange={(e) => { setAdminLogSearch(e.target.value); setAdminLogPage(1); }}
                  className="custom-input"
                  style={{ paddingLeft: 34, fontSize: 13, height: 38, width: 220 }}
                />
                <span style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: "var(--text-secondary)", display: "grid", placeItems: "center" }}>
                  <SearchIcon />
                </span>
              </div>
            </div>

            <div style={{ overflowX: "auto" }}>
              {(() => {
                const filteredLogs = adminActionLogs.filter(log =>
                  log.admin_email.toLowerCase().includes(adminLogSearch.toLowerCase()) ||
                  log.action_type.toLowerCase().includes(adminLogSearch.toLowerCase()) ||
                  log.target_type.toLowerCase().includes(adminLogSearch.toLowerCase()) ||
                  log.target_name.toLowerCase().includes(adminLogSearch.toLowerCase()) ||
                  (log.details && log.details.toLowerCase().includes(adminLogSearch.toLowerCase()))
                );
                const totalLogPages = Math.ceil(filteredLogs.length / itemsPerPage) || 1;
                const paginatedLogs = filteredLogs.slice((adminLogPage - 1) * itemsPerPage, adminLogPage * itemsPerPage);

                if (paginatedLogs.length === 0) {
                  return (
                    <div style={{ padding: 40, textAlign: "center", color: "var(--text-secondary)" }}>
                      <p style={{ fontSize: 24, margin: "0 0 8px" }}>📜</p>
                      <p style={{ margin: 0, fontWeight: 500 }}>No administrative actions recorded yet.</p>
                    </div>
                  );
                }

                return (
                  <>
                    <table className="custom-table">
                      <thead>
                        <tr>
                          <th>Admin Email</th>
                          <th>Action</th>
                          <th>Target Details</th>
                          <th>Log Details</th>
                          <th style={{ textAlign: "right" }}>Timestamp</th>
                        </tr>
                      </thead>
                      <tbody>
                        {paginatedLogs.map(log => {
                          const isAdd = log.action_type.toUpperCase() === "ADD";
                          const isEdit = log.action_type.toUpperCase() === "EDIT";
                          const isDelete = log.action_type.toUpperCase() === "DELETE";

                          const badgeColor = isAdd ? "var(--success)" : isEdit ? "var(--info)" : "var(--danger)";
                          const badgeBg = isAdd ? "var(--success-light)" : isEdit ? "var(--info-light)" : "var(--danger-light)";

                          return (
                            <tr key={log.id}>
                              <td style={{ fontWeight: 600, color: "var(--text-primary)", fontSize: 13.5 }}>
                                {log.admin_email}
                              </td>
                              <td>
                                <span style={{
                                  display: "inline-flex",
                                  padding: "4px 8px",
                                  borderRadius: 6,
                                  fontSize: 11.5,
                                  fontWeight: 700,
                                  background: badgeBg,
                                  color: badgeColor
                                }}>
                                  {log.action_type}
                                </span>
                              </td>
                              <td>
                                <div style={{ fontWeight: 600, fontSize: 13.5, color: "var(--text-primary)" }}>{log.target_name}</div>
                                <div style={{ fontSize: 11.5, color: "var(--text-secondary)", marginTop: 2, textTransform: "uppercase", letterSpacing: 0.5 }}>
                                  {log.target_type}
                                </div>
                              </td>
                              <td style={{ color: "var(--text-secondary)", fontSize: 13, maxWidth: 300, overflow: "hidden", textOverflow: "ellipsis" }}>
                                {log.details}
                              </td>
                              <td style={{ textAlign: "right", color: "var(--text-secondary)", fontSize: 13.5 }}>
                                <div>{formatDate(log.created_at)}</div>
                                <div style={{ fontSize: 11, marginTop: 4 }}>{formatTime(log.created_at)}</div>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>

                    {totalLogPages > 1 && (
                      <div style={{ padding: "16px 24px", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>
                          Page {adminLogPage} of {totalLogPages}
                        </span>
                        <div style={{ display: "flex", gap: 8 }}>
                          <button
                            disabled={adminLogPage === 1}
                            onClick={() => setAdminLogPage(prev => Math.max(1, prev - 1))}
                            className="theme-btn"
                            style={{ height: 32, padding: "0 10px" }}
                          >
                            <ChevronLeftIcon /> Prev
                          </button>
                          <button
                            disabled={adminLogPage === totalLogPages}
                            onClick={() => setAdminLogPage(prev => Math.min(totalLogPages, prev + 1))}
                            className="theme-btn"
                            style={{ height: 32, padding: "0 10px" }}
                          >
                            Next <ChevronRightIcon />
                          </button>
                        </div>
                      </div>
                    )}
                  </>
                );
              })()}
            </div>
          </article>
        )}
      </main>

      {/* ========================================== */}
      {/* MODAL: EDIT EMPLOYEE PROFILE */}
      {/* ========================================== */}
      {editingEmployee && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Edit Profile Settings</h3>
              <button
                onClick={() => setEditingEmployee(null)}
                style={{ background: "none", border: "none", cursor: "pointer", color: "var(--text-secondary)", padding: 4 }}
              >
                <CloseIcon />
              </button>
            </div>

            {editErrorMsg && (
              <div style={{ background: "var(--danger-light)", color: "var(--danger-text)", padding: "10px 14px", borderRadius: 8, marginBottom: 16, fontSize: 13 }}>
                {editErrorMsg}
              </div>
            )}

            <form onSubmit={handleEditEmployeeSubmit} style={{ display: "grid", gap: 16 }}>
              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  Full Name
                </label>
                <input
                  type="text"
                  value={editForm.full_name}
                  onChange={(e) => setEditForm({ ...editForm, full_name: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  Email Address
                </label>
                <input
                  type="email"
                  value={editForm.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  Phone Number
                </label>
                <input
                  type="text"
                  placeholder="e.g. +91 9876543210"
                  value={editForm.phone}
                  onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  System Role
                </label>
                <select
                  value={editForm.role}
                  onChange={(e) => setEditForm({ ...editForm, role: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%", background: "var(--bg-card)" }}
                >
                  <option value="employee">Employee (Faculty/Staff)</option>
                  <option value="admin">System Administrator</option>
                </select>
              </div>

              <div style={{ display: "flex", gap: 12, marginTop: 12 }}>
                <button
                  type="submit"
                  className="theme-btn"
                  style={{ flex: 1, justifyContent: "center", background: "var(--primary)", color: "white", border: "none" }}
                >
                  Save Changes
                </button>
                <button
                  type="button"
                  onClick={() => setEditingEmployee(null)}
                  className="theme-btn"
                  style={{ flex: 1, justifyContent: "center" }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================== */}
      {/* MODAL: ADD EMPLOYEE / ADMIN */}
      {/* ========================================== */}
      {addEmployeeOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Register New User Profile</h3>
              <button
                onClick={() => setAddEmployeeOpen(false)}
                style={{ background: "none", border: "none", cursor: "pointer", color: "var(--text-secondary)", padding: 4 }}
              >
                <CloseIcon />
              </button>
            </div>

            <form onSubmit={handleAddEmployeeSubmit} style={{ display: "grid", gap: 16 }}>
              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  Full Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="John Doe"
                  value={addEmployeeForm.full_name}
                  onChange={(e) => setAddEmployeeForm({ ...addEmployeeForm, full_name: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  Email Address *
                </label>
                <input
                  type="email"
                  required
                  placeholder="johndoe@nitc.ac.in"
                  value={addEmployeeForm.email}
                  onChange={(e) => setAddEmployeeForm({ ...addEmployeeForm, email: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  Password *
                </label>
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={addEmployeeForm.password}
                  onChange={(e) => setAddEmployeeForm({ ...addEmployeeForm, password: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  Phone Number
                </label>
                <input
                  type="text"
                  placeholder="e.g. +91 9876543210"
                  value={addEmployeeForm.phone}
                  onChange={(e) => setAddEmployeeForm({ ...addEmployeeForm, phone: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: 12.5, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 6 }}>
                  Portal Authorization Role
                </label>
                <select
                  value={addEmployeeForm.role}
                  onChange={(e) => setAddEmployeeForm({ ...addEmployeeForm, role: e.target.value })}
                  className="custom-input"
                  style={{ width: "100%", background: "var(--bg-card)" }}
                >
                  <option value="employee">Employee (Faculty/Staff)</option>
                  <option value="admin">System Administrator</option>
                </select>
              </div>

              <div style={{ display: "flex", gap: 12, marginTop: 12 }}>
                <button
                  type="submit"
                  className="theme-btn"
                  style={{ flex: 1, justifyContent: "center", background: "var(--primary)", color: "white", border: "none" }}
                >
                  Add Profile
                </button>
                <button
                  type="button"
                  onClick={() => setAddEmployeeOpen(false)}
                  className="theme-btn"
                  style={{ flex: 1, justifyContent: "center" }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================== */}
      {/* MODAL: REPLY TO SUPPORT TICKET */}
      {/* ========================================== */}
      {replyTicket && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: 540 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "var(--text-primary)" }}>Support Assistance Portal</h3>
              <button
                onClick={() => { setReplyTicket(null); setReplyText(""); }}
                style={{ background: "none", border: "none", cursor: "pointer", color: "var(--text-secondary)", padding: 4 }}
              >
                <CloseIcon />
              </button>
            </div>

            <div style={{
              background: "var(--table-header-bg)",
              border: "1px solid var(--border-color)",
              padding: 16,
              borderRadius: 12,
              marginBottom: 20,
              maxHeight: 180,
              overflowY: "auto"
            }}>
              <div style={{ fontSize: 12.5, fontWeight: 700, color: "var(--text-primary)", marginBottom: 8 }}>
                QUERY FROM {replyTicket.user_name} ({replyTicket.user_email}):
              </div>
              <div style={{ fontSize: 13.5, color: "var(--text-secondary)", whiteSpace: "pre-wrap", lineHeight: 1.5 }}>
                {replyTicket.message}
              </div>
            </div>

            <form onSubmit={handleSendReply} style={{ display: "grid", gap: 16 }}>
              <div>
                <label style={{ display: "block", fontSize: 13, fontWeight: 600, color: "var(--text-secondary)", marginBottom: 8 }}>
                  Response Message
                </label>
                <textarea
                  required
                  rows="4"
                  placeholder="Type your reply to the employee here..."
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  className="custom-input"
                  style={{
                    width: "100%",
                    padding: 12,
                    fontSize: 13.5,
                    lineHeight: 1.5,
                    resize: "vertical",
                    fontFamily: "var(--font-sans)"
                  }}
                />
              </div>

              <div style={{ display: "flex", gap: 12 }}>
                <button
                  type="submit"
                  className="theme-btn"
                  style={{ flex: 1, justifyContent: "center", background: "var(--primary)", color: "white", border: "none" }}
                >
                  Send Response
                </button>
                <button
                  type="button"
                  onClick={() => { setReplyTicket(null); setReplyText(""); }}
                  className="theme-btn"
                  style={{ flex: 1, justifyContent: "center" }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================== */}
      {/* MODAL: CUSTOM CONFIRMATION OVERLAY */}
      {/* ========================================== */}
      {confirmDialog.isOpen && (
        <div style={{
          position: "fixed",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          background: "rgba(0, 0, 0, 0.4)",
          backdropFilter: "blur(4px)",
          display: "grid",
          placeItems: "center",
          zIndex: 9998
        }}>
          <div style={{
            background: "var(--bg-card)",
            border: "1px solid var(--border-color)",
            borderRadius: 20,
            padding: 32,
            maxWidth: 480,
            width: "90%",
            boxShadow: "var(--shadow-lg)",
            animation: "scaleUp 0.25s ease forwards"
          }}>
            <div style={{ display: "flex", gap: 16, alignItems: "flex-start", marginBottom: 24 }}>
              <div style={{
                background: "var(--danger-light)",
                color: "var(--danger-text)",
                width: 48,
                height: 48,
                borderRadius: 14,
                display: "grid",
                placeItems: "center",
                flexShrink: 0
              }}>
                <AlertIcon />
              </div>
              <div style={{ flex: 1 }}>
                <h3 style={{ margin: 0, fontSize: 18, color: "var(--text-primary)", fontWeight: 700 }}>{confirmDialog.title}</h3>
                <p style={{ margin: "8px 0 0", color: "var(--text-secondary)", fontSize: 14, lineHeight: 1.5 }}>
                  {confirmDialog.message}
                </p>
              </div>
            </div>
            <div style={{ display: "flex", gap: 12, justifyContent: "flex-end" }}>
              <button
                onClick={() => setConfirmDialog({ isOpen: false, title: "", message: "", onConfirm: null })}
                style={{
                  padding: "10px 20px",
                  borderRadius: 10,
                  border: "1px solid var(--border-color)",
                  background: "transparent",
                  color: "var(--text-secondary)",
                  fontWeight: 600,
                  cursor: "pointer",
                  fontSize: 13
                }}
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  confirmDialog.onConfirm();
                  setConfirmDialog({ isOpen: false, title: "", message: "", onConfirm: null });
                }}
                style={{
                  padding: "10px 20px",
                  borderRadius: 10,
                  border: "none",
                  background: "var(--danger)",
                  color: "white",
                  fontWeight: 600,
                  cursor: "pointer",
                  fontSize: 13
                }}
              >
                Confirm
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================== */}
      {/* NOTIFICATION: CUSTOM TOAST PORTAL */}
      {/* ========================================== */}
      {toast && (
        <div style={{
          position: "fixed",
          top: 24,
          right: 24,
          background: toast.type === "error" ? "var(--danger)" : toast.type === "info" ? "var(--info)" : "var(--success)",
          color: "white",
          padding: "16px 24px",
          borderRadius: 12,
          boxShadow: "var(--shadow-lg)",
          zIndex: 9999,
          display: "flex",
          alignItems: "center",
          gap: 12,
          fontWeight: 600,
          fontSize: 14,
          animation: "slideIn 0.3s ease forwards"
        }}>
          {toast.type === "error" ? <AlertIcon /> : <CheckIcon />}
          <span style={{ flex: 1 }}>{toast.message}</span>
          <button
            onClick={() => setToast(null)}
            style={{ background: "transparent", border: "none", color: "white", cursor: "pointer", display: "grid", placeItems: "center", padding: 0 }}
          >
            <CloseIcon />
          </button>
        </div>
      )}
    </div>
  );
}

export default Dashboard;