import Dashboard from "./components/Dashboard.vue"
import Login from "./components/Login.vue"
import SystemManagement from './components/SystemManagement.vue'
import AddUser from './components/AddUser.vue'
import DelUser from './components/DelUser.vue'
import UpdateUser from './components/UpdateUser.vue'
import VehicleOps from './components/VehicleOps.vue'

export const routes = [
    {path: '/', component: Dashboard, name: 'Dashboard'},
    {path: '/Login', component: Login, name: 'Login'},
    {
        path: '/Vehicle', component: VehicleOps, name: 'VehicleOps',
        children: [
            {path: 'AddVehicle', component:AddUser, name:'AddVehicle' },
            {path: 'DelVehicle', component:DelUser, name:'DelVehicle' },
            {path: 'UpdateVehicle', component:UpdateUser, name:'UpdateVehicle' },
        ]
    },
    {
        path: '/System', component: SystemManagement, name: 'SystemManagement',
        children: [
            {path: 'AddUser', component:AddUser, name:'AddUser' },
            {path: 'DelUser', component:DelUser, name:'DelUser' },
            {path: 'UpdateUser', component:UpdateUser, name:'UpdateUser' },
        ]
    },
    {path: '*', redirect: '/'}
];
