import Dashboard from "./components/Dashboard.vue"
import Login from "./components/Login.vue"
import SystemManagement from './components/SystemManagement.vue'
import AddUser from './components/AddUser.vue'
import DelUser from './components/DelUser.vue'
import UpdateUser from './components/UpdateUser.vue'
import VehicleOps from './components/VehicleOps.vue'

import Order from './components/Order.vue'
import NewOrder from './components/NewOrder.vue'
import UnhandledOrder from './components/UnhandledOrder.vue'
import MyOrder from "./components/MyOrder.vue"
import QueryOrder from "./components/QueryOrder.vue"

import OilUser from './components/OilUser.vue'
import QueryOilUser from './components/QueryOilUser.vue'

import Intern from './components/Intern.vue'
import UpdateBuilder from './components/UpdateBuilder.vue'
import UpdateBuildCase from './components/UpdateBuildCase.vue'
import UsageReport from './components/UsageReport.vue'

import WorkPointMap from './components/WorkPointMap.vue'
import UpdateContractor from './components/UpdateContractor.vue'

import Download from './components/Download.vue'
import CaseList from './components/CaseList.vue'

export const routes = [{
        path: '/',
        component: Dashboard,
        name: 'Dashboard'
    },
    {
        path: '/Login',
        component: Login,
        name: 'Login'
    },
    {
        path: '/OilUser',
        component: OilUser,
        name: 'OilUser',
        children: [{
            path: 'Query',
            component: QueryOilUser,
            name: 'QueryOilUser'
        }]
    },
    {
        path: '/Intern',
        component: Intern,
        name: 'Intern',
        children: [{
                path: 'Builder',
                component: UpdateBuilder,
                name: 'UpdateBuilder'
            },
            {
                path: 'BuildCase',
                component: UpdateBuildCase,
                name: 'UpdateBuildCase'
            },
            {
                path: 'Contractor',
                component: UpdateContractor,
                name: 'UpdateContractor'
            },
            {
                path: 'Report',
                component: UsageReport,
                name: 'Report'
            },
        ]
    },
    {
        path: '/Sales',
        component: Intern,
        name: 'Sales',
        children: [{
                path: 'MyCase',
                component: CaseList,
                name: 'MyCase',
                props: true
            },
            {
                path: 'ObtainCase',
                component: CaseList,
                name: 'ObtainCase',
                props: true
            },
            {
                path: 'Map',
                component: WorkPointMap,
                name: 'Map'
            },
            {
                path: 'UsageReport',
                component: UsageReport,
                name: 'UsageReport'
            }
        ]
    },
    {
        path: '/SalesAdmin',
        component: Intern,
        name: 'SalesAdmin',
        children: [{
            path: 'CaseList',
            component: CaseList,
            name: 'CaseList',
            props: true
        }]
    },
    {
        path: '/System',
        component: SystemManagement,
        name: 'SystemManagement',
        children: [{
                path: 'AddUser',
                component: AddUser,
                name: 'AddUser'
            },
            {
                path: 'DelUser',
                component: DelUser,
                name: 'DelUser'
            },
            {
                path: 'UpdateUser',
                component: UpdateUser,
                name: 'UpdateUser'
            },
            {
                path: 'Download',
                component: Download,
                name: 'Download'
            }
        ]
    },
    {
        path: '*',
        redirect: '/'
    }
];